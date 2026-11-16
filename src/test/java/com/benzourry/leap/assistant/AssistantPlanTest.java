package com.benzourry.leap.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for the parts of the assistant that do not need a model or a database: the plan
 * sanitiser (the only thing standing between a model and the executor on the client) and the
 * answer reader.
 */
class AssistantPlanTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode sanitize(String json, boolean streaming, int maxActions) throws Exception {
        JsonNode plan = mapper.readTree(json);
        return new AssistantPlanSanitizer(mapper, maxActions).sanitize(plan, streaming);
    }

    @Test
    void keepsValidStepsAndDropsUnknownKinds() throws Exception {
        ObjectNode out = sanitize("""
                {"message": "ok", "actions": [
                  {"id": "form_leave", "kind": "create_form", "payload": {"title": "Leave"}},
                  {"id": "wipe", "kind": "drop_database", "payload": {}},
                  {"id": "sec_main", "kind": "add_section",
                   "payload": {"formRef": "$form_leave", "title": "Details", "type": "section"}}
                ]}
                """, false, 60);

        assertEquals(2, out.path("actions").size());
        assertEquals("create_form", out.path("actions").get(0).path("kind").asText());
        assertEquals("add_section", out.path("actions").get(1).path("kind").asText());
        assertEquals("Details", out.path("actions").get(1).path("title").asText());
        assertTrue(out.path("warnings").get(0).asText().contains("1 proposed step"));
    }

    @Test
    void dropsStepsThatCannotBeExecuted() throws Exception {
        ObjectNode out = sanitize("""
                {"message": "ok", "actions": [
                  {"id": "a", "kind": "create_form", "payload": {}},
                  {"id": "b", "kind": "add_section_item",
                   "payload": {"formRef": "$a", "sectionRef": "$s", "label": "Reason", "type": "text"}},
                  {"id": "c", "kind": "add_section_item",
                   "payload": {"formRef": "$a", "sectionRef": "$s", "label": "No type"}},
                  {"id": "d", "kind": "create_screen", "payload": {"title": "Home"}},
                  {"id": "e", "kind": "update_item",
                   "payload": {"formRef": 1, "itemRef": 2, "patch": {}}}
                ]}
                """, false, 60);

        assertEquals(1, out.path("actions").size());
        assertEquals("b", out.path("actions").get(0).path("id").asText());
    }

    @Test
    void dropsRepeatedIdsAndRespectsTheCap() throws Exception {
        ObjectNode out = sanitize("""
                {"message": "ok", "actions": [
                  {"id": "same", "kind": "create_form", "payload": {"title": "One"}},
                  {"id": "same", "kind": "create_form", "payload": {"title": "Two"}},
                  {"id": "third", "kind": "create_dashboard", "payload": {"title": "Board"}}
                ]}
                """, false, 2);

        assertEquals(2, out.path("actions").size());
        assertEquals("One", out.path("actions").get(0).path("payload").path("title").asText());
        assertEquals("third", out.path("actions").get(1).path("id").asText());
    }

    @Test
    void flagsReferencesToStepsThatAreNotInThePlan() throws Exception {
        ObjectNode out = sanitize("""
                {"message": "ok", "actions": [
                  {"id": "sec", "kind": "add_section",
                   "payload": {"formRef": "$ghost", "title": "Details", "type": "section"}}
                ]}
                """, false, 60);

        String warnings = out.path("warnings").toString();
        assertTrue(warnings.contains("$ghost"), warnings);
    }

    @Test
    void flagsReferencesToLaterSteps() throws Exception {
        ObjectNode out = sanitize("""
                {"message": "ok", "actions": [
                  {"id": "sec", "kind": "add_section",
                   "payload": {"formRef": "$form", "title": "Details", "type": "section"}},
                  {"id": "form", "kind": "create_form", "payload": {"title": "Leave"}}
                ]}
                """, false, 60);

        assertTrue(out.path("warnings").toString().contains("sec -> $form"));
    }

    @Test
    void marksRemovalsAsDestructive() throws Exception {
        ObjectNode out = sanitize("""
                {"message": "ok", "actions": [
                  {"id": "r", "kind": "remove_screen", "payload": {"screenRef": 9}}
                ]}
                """, false, 60);

        assertTrue(out.path("actions").get(0).path("destructive").asBoolean());
    }

    @Test
    void streamingModeClearsTheMessageSoTheProseIsShown() throws Exception {
        ObjectNode streamed = sanitize("{\"message\": \"summary\", \"actions\": []}", true, 60);
        assertEquals("", streamed.path("message").asText());

        ObjectNode blocking = sanitize("{\"actions\": []}", false, 60);
        assertFalse(blocking.path("message").asText().isBlank());
    }

    @Test
    void unreadablePlanBecomesAnEmptyPlanWithAWarning() {
        ObjectNode out = new AssistantPlanSanitizer(mapper, 60).sanitize(null, true);
        assertEquals(0, out.path("actions").size());
        assertTrue(out.path("warnings").get(0).asText().contains("readable plan"));
    }

    @Test
    void readsBareFencedAndEmbeddedJson() {
        AssistantService service = new AssistantService(new AssistantProperties(), new AssistantModelFactory(), mapper);

        JsonNode bare = service.parseJsonObject("{\"actions\": []}");
        assertNotNull(bare);
        assertEquals(0, bare.path("actions").size());

        JsonNode fenced = service.parseJsonObject("```json\n{\"actions\": [{\"id\": \"a\"}]}\n```");
        assertNotNull(fenced);
        assertEquals(1, fenced.path("actions").size());

        JsonNode embedded = service.parseJsonObject(
                "Here is the plan.\n\n<!--PLAN-->\n{\"actions\": [{\"id\": \"a\"}, {\"id\": \"b\"}]}");
        assertNotNull(embedded);
        assertEquals(2, embedded.path("actions").size());

        assertNull(service.parseJsonObject("no json at all"));
        assertNull(service.parseJsonObject(""));
        assertNull(service.parseJsonObject(null));
    }

    @Test
    void systemPromptHasNoUnresolvedPlaceholders() {
        AssistantProperties properties = new AssistantProperties();
        String streaming = AssistantPrompts.systemPrompt(properties, true);
        assertTrue(streaming.contains(AssistantPrompts.PLAN_MARKER));
        assertTrue(streaming.contains(String.valueOf(properties.getMaxActions())));
        assertFalse(streaming.contains("@MAX_ACTIONS@"));
        assertTrue(streaming.contains("A form is not complete with only a create_form action."));
        assertTrue(streaming.contains("add_section_item action for every"));

        String blocking = AssistantPrompts.systemPrompt(properties, false);
        assertFalse(blocking.contains(AssistantPrompts.PLAN_MARKER));
    }

    @Test
    void flagsNestedReferencesToStepsThatAreNotInThePlan() throws Exception {
        ObjectNode out = sanitize("""
                {"message": "ok", "actions": [
                  {"id": "dash", "kind": "create_dashboard", "payload": {"title": "Board"}},
                  {"id": "chart", "kind": "add_chart",
                   "payload": {"dashboardRef": "$dash", "title": "Count", "type": "bar",
                               "series": [{"formRef": "$missing"}]}}
                ]}
                """, false, 60);

        String warnings = out.path("warnings").toString();
        assertTrue(warnings.contains("$missing"), warnings);
        assertFalse(warnings.contains("$dash"), warnings);
    }

    @Test
    void usesTheCreatedNameWhenTheStepHasNoTitle() throws Exception {
        ObjectNode out = sanitize("""
                {"actions": [
                  {"id": "fld_note", "kind": "add_section_item",
                   "payload": {"formRef": 1, "sectionRef": 2, "label": "Note", "type": "text"}}
                ]}
                """, false, 60);

        assertEquals("Note", out.path("actions").get(0).path("title").asText());
    }

    @Test
    void humanisesTheIdWhenThereIsNoNameAtAll() throws Exception {
        ObjectNode out = sanitize("""
                {"actions": [
                  {"id": "sec_main", "kind": "remove_section",
                   "payload": {"formRef": 1, "sectionRef": 2}}
                ]}
                """, false, 60);

        assertEquals("Sec main", out.path("actions").get(0).path("title").asText());
    }

    @Test
    void userPromptCarriesTheContextItWasGiven() throws Exception {
        AssistantProperties properties = new AssistantProperties();
        JsonNode context = mapper.readTree("""
                {"app": {"id": 7, "title": "HR"},
                 "entities": {"forms": [{"id": 11, "title": "Leave"}]},
                 "knowledge": {"sizes": ["col-sm-12"]}}
                """);
        AssistantDtos.PlanRequest request = new AssistantDtos.PlanRequest(
                "build a leave form",
                List.of(new AssistantDtos.ChatMessage("user", "hello"),
                        new AssistantDtos.ChatMessage("assistant", "hi")),
                context,
                new AssistantDtos.ModelSettings("https://api.example.com/v1", "model", "key"));

        String prompt = AssistantPrompts.userPrompt(request, properties);
        assertTrue(prompt.contains("HR"));
        assertTrue(prompt.contains("Leave"));
        assertTrue(prompt.contains("col-sm-12"));
        assertTrue(prompt.contains("build a leave form"));
        assertTrue(prompt.contains("assistant: hi"));
    }
}
