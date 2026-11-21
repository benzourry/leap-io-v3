package com.benzourry.leap.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates whatever the model produced before it reaches the browser.
 *
 * <p>The executor on the client trusts the plan it receives, so this is the single place where
 * unknown kinds, malformed payloads, duplicate ids, and plans longer than the configured cap are
 * removed. Missing or out-of-order step references are reported so the browser can block them.
 * </p>
 */
final class AssistantPlanSanitizer {

    static final Set<String> KINDS = Set.of(
            "create_form",
            "create_tab",
            "update_tab",
            "remove_tab",
            "assign_section_to_tab",
            "update_form",
            "add_section",
            "update_section",
            "add_section_item",
            "update_item",
            "remove_item",
            "remove_section",
            "create_tier",
            "update_tier",
            "remove_tier",
            "add_tier_action",
            "update_tier_action",
            "remove_tier_action",
            "create_dataset",
            "add_dataset_item",
            "update_dataset_item",
            "remove_dataset_item",
            "add_dataset_action",
            "update_dataset_action",
            "remove_dataset_action",
            "add_dataset_filter",
            "update_dataset_filter",
            "remove_dataset_filter",
            "create_dashboard",
            "add_chart",
            "create_screen",
            "add_screen_action",
            "update_screen_action",
            "remove_screen_action",
            "update_screen",
            "remove_screen",
            "create_lambda",
            "update_lambda",
            "remove_lambda",
            "add_lambda_binding",
            "remove_lambda_binding",
            "create_email_template",
            "update_email_template",
            "remove_email_template",
            "create_navi_group",
            "update_navi_group",
            "remove_navi_group",
            "add_navi_item",
            "update_navi_item",
            "remove_navi_item");

    private static final Set<String> DESTRUCTIVE = Set.of(
            "remove_tab", "remove_item", "remove_section", "remove_tier", "remove_dataset_item",
            "remove_tier_action", "remove_dataset_action", "remove_dataset_filter",
            "remove_screen_action", "remove_screen", "remove_lambda", "remove_lambda_binding",
            "remove_email_template", "remove_navi_group", "remove_navi_item");
    private static final Set<String> PATCH_ACTIONS = Set.of(
            "update_tab", "update_form", "update_section", "update_item", "update_tier",
            "update_tier_action", "update_dataset_item", "update_dataset_action",
            "update_dataset_filter", "update_screen_action", "update_lambda",
            "update_email_template", "update_navi_group", "update_navi_item");

    // Matches <script>...</script> blocks, case-insensitive, including newlines
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final ObjectMapper mapper;
    private final int maxActions;

    AssistantPlanSanitizer(ObjectMapper mapper, int maxActions) {
        this.mapper = mapper;
        this.maxActions = Math.max(1, maxActions);
    }

    /**
     * @param plan      raw JSON produced by the model; may be null
     * @param streaming when true the visible message comes from the streamed prose, so the JSON
     *                  "message" field is cleared to let the frontend prefer the prose
     */
    ObjectNode sanitize(JsonNode plan, boolean streaming) {
        ObjectNode out = mapper.createObjectNode();
        out.put("planId", UUID.randomUUID().toString());

        System.out.println("Sanitizing plan: " + (plan == null ? "null" : plan.toString()));

        String message = text(plan == null ? null : plan.get("message"));

        ArrayNode actions = out.putArray("actions");
        List<String> warnings = new ArrayList<>();

        JsonNode proposed = plan == null ? null : plan.get("actions");
        if (proposed == null || !proposed.isArray()) {
            warnings.add("The assistant did not return a readable plan. Try describing what you need "
                    + "in a little more detail.");
        } else {
            Set<String> kept = new HashSet<>();
            int dropped = 0;
            for (JsonNode raw : proposed) {
                if (actions.size() >= maxActions) {
                    dropped++;
                    continue;
                }
                String reason = problem(raw);
                if (reason != null) {
                    dropped++;
                    continue;
                }
                String kind = raw.path("kind").asText();
                String id = text(raw.get("id"));
                if (id.isBlank() || !kept.add(id)) {
                    dropped++;
                    continue;
                }
                ObjectNode action = actions.addObject();
                action.put("id", id);
                action.put("kind", kind);
                String title = text(raw.get("title"));
                if (title.isBlank()) {
                    // The review list is driven by this label, so fall back to what the step creates.
                    title = text(raw.path("payload").get("title"));
                }
                if (title.isBlank()) {
                    title = text(raw.path("payload").get("label"));
                }
                if (title.isBlank()) {
                    title = text(raw.path("payload").get("name"));
                }
                action.put("title", title.isBlank() ? humanise(id) : title);
                String note = text(raw.get("note"));
                if (!note.isBlank()) {
                    action.put("note", note);
                }
                if (DESTRUCTIVE.contains(kind) || raw.path("destructive").asBoolean(false)) {
                    action.put("destructive", true);
                }

                ObjectNode safePayload = ((ObjectNode) raw.get("payload")).deepCopy();

                if ("create_screen".equals(kind)) {
                    if ("combined".equalsIgnoreCase(safePayload.path("type").asText())) {
                        safePayload.put("type", "combine");
                    }
                    extractScriptToF(safePayload);
                } else if ("update_screen".equals(kind)) {
                    if (safePayload.has("patch") && safePayload.get("patch").isObject()) {
                        extractScriptToF((ObjectNode) safePayload.get("patch"));
                    } else {
                        extractScriptToF(safePayload);
                    }
                }

                action.set("payload", safePayload);
            }

            if (dropped > 0) {
                warnings.add("The assistant could not prepare some of the requested steps. Ask it to revise the plan.");
            }
            if (!kept.isEmpty()) {
                List<String> dangling = danglingReferences(actions);
                if (!dangling.isEmpty()) {
                    warnings.add("Some steps need another step first, but it is missing or out of order. "
                            + "Ask the assistant to update the plan before applying.");
                }
            }
        }

        JsonNode incoming = plan == null ? null : plan.get("warnings");
        if (incoming != null && incoming.isArray()) {
            for (JsonNode w : incoming) {
                String line = w.isTextual() ? w.asText().trim() : "";
                if (!line.isBlank() && warnings.size() < 6) {
                    warnings.add(line);
                }
            }
        }

        if (streaming) {
            message = "";
        } else if (message.isBlank()) {
            message = actions.isEmpty()
                    ? "I could not turn that into a plan. Try describing the form or screen you need "
                    + "in a little more detail."
                    : "Here is what I propose.";
        }
        out.put("message", message);

        boolean needsUserInput = plan != null && plan.path("needsUserInput").asBoolean(false);
        String clarificationQuestion = text(plan == null ? null : plan.get("clarificationQuestion"));
        if (clarificationQuestion.length() > 1000) {
            clarificationQuestion = clarificationQuestion.substring(0, 1000);
        }
        needsUserInput = needsUserInput || !clarificationQuestion.isBlank();
        out.put("needsUserInput", needsUserInput);
        if (!clarificationQuestion.isBlank()) {
            out.put("clarificationQuestion", clarificationQuestion);
        }

        ArrayNode warn = out.putArray("warnings");
        for (String w : warnings) {
            warn.add(w);
        }
        return out;
    }

    /**
     * Extracts <script> tags from 'content' and appends them to 'f',
     * modifying the object payload in place.
     */
    private static void extractScriptToF(ObjectNode node) {
        JsonNode contentNode = node.get("content");
        if (contentNode != null && contentNode.isTextual()) {
            String content = contentNode.asText();
            Matcher matcher = SCRIPT_PATTERN.matcher(content);

            if (matcher.find()) {
                StringBuilder scriptBuilder = new StringBuilder();
                String existingF = node.path("f").asText("");
                if (!existingF.isBlank()) {
                    scriptBuilder.append(existingF).append("\n");
                }

                matcher.reset();
                while (matcher.find()) {
                    String scriptBody = matcher.group(1).trim();
                    if (!scriptBody.isEmpty()) {
                        scriptBuilder.append(scriptBody).append("\n");
                    }
                }

                // Remove the script tags from the original HTML content
                node.put("content", matcher.replaceAll("").trim());
                node.put("f", scriptBuilder.toString().trim());
            }
        }
    }

    /** Returns a human readable reason when the action cannot be executed, otherwise null. */
    private String problem(JsonNode raw) {
        if (raw == null || !raw.isObject()) {
            return "not an object";
        }
        String kind = raw.path("kind").asText("");
        if (!KINDS.contains(kind)) {
            return "unknown kind";
        }
        JsonNode payload = raw.get("payload");
        if (payload == null || !payload.isObject()) {
            return "missing payload";
        }
        for (String field : required(kind)) {
            if (isBlank(payload.get(field))) {
                return "missing " + field;
            }
        }
        if ("create_screen".equals(kind)) {
            String screenType = payload.path("type").asText("static");
            if ("combine".equalsIgnoreCase(screenType) || "combined".equalsIgnoreCase(screenType)) {
                JsonNode comps = payload.get("comps");
                if (comps == null || !comps.isArray() || comps.isEmpty()) {
                    return "combined screen without components";
                }
                for (JsonNode comp : comps) {
                    String componentType = comp.path("type").asText("");
                    if (!Set.of("add", "edit-single", "view-single", "dataset", "dashboard",
                            "screen", "lookup", "user").contains(componentType)) {
                        return "combined screen with an invalid component";
                    }
                    if (!"user".equals(componentType) && isBlank(comp.get("targetRef"))) {
                        return "combined screen component without a target";
                    }
                }
            } else if (!payload.path("content").isTextual()) {
                return "screen without a template";
            }
        }
        if ("add_section_item".equals(kind) && !payload.path("type").isTextual()) {
            return "field without a type";
        }
        if ("add_screen_action".equals(kind)
                && !"function".equals(payload.path("nextType").asText())
                && isBlank(payload.get("targetRef"))) {
            return "screen action without a target";
        }
        if ("add_navi_item".equals(kind)) {
            String type = payload.path("type").asText();
            if (Set.of("form", "form-single", "view-single", "dataset", "dashboard", "screen", "lookup")
                    .contains(type) && isBlank(payload.get("targetRef"))) {
                return "navigation item without a target";
            }
            if ("add_lambda_binding".equals(kind)) {
                JsonNode binding = payload.get("binding");
                if (binding == null || !binding.isObject()
                        || isBlank(binding.get("name")) || isBlank(binding.get("type"))) {
                    return "lambda binding without a name or type";
                }
            }
            if ("create_email_template".equals(kind)
                    && (isBlank(payload.get("name"))
                        || isBlank(payload.get("subject"))
                        || isBlank(payload.get("content")))) {
                return "email template without required content";
            }
            if (Set.of("external", "internal", "lambda").contains(type) && isBlank(payload.get("url"))) {
                return "navigation item without a URL";
            }
        }
        if (PATCH_ACTIONS.contains(kind)) {
            JsonNode patch = payload.get("patch");
            if (patch == null || !patch.isObject() || patch.isEmpty()) {
                return "empty patch";
            }
        }
        return null;
    }

    private static List<String> required(String kind) {
        switch (kind) {
            case "create_form":
                return List.of("title");
            case "create_tab":
                return List.of("formRef", "title");
            case "update_tab":
                return List.of("formRef", "tabRef");
            case "remove_tab":
                return List.of("formRef", "tabRef");
            case "assign_section_to_tab":
                return List.of("formRef", "sectionRef", "tabRef");
            case "update_form":
                return List.of("formRef");
            case "add_section":
                return List.of("formRef", "title");
            case "update_section":
                return List.of("formRef", "sectionRef");
            case "add_section_item":
                return List.of("formRef", "sectionRef", "label");
            case "update_item":
                return List.of("formRef", "itemRef");
            case "remove_item":
                return List.of("formRef", "itemRef");
            case "remove_section":
                return List.of("formRef", "sectionRef");
            case "create_tier":
                return List.of("formRef", "name");
            case "update_tier":
                return List.of("formRef", "tierRef");
            case "remove_tier":
                return List.of("formRef", "tierRef");
            case "add_tier_action":
                return List.of("formRef", "tierRef", "label", "action");
            case "update_tier_action":
            case "remove_tier_action":
                return List.of("formRef", "tierRef", "actionRef");
            case "create_dataset":
                return List.of("title");
            case "add_dataset_item":
                return List.of("datasetRef", "label");
            case "update_dataset_item":
                return List.of("datasetRef", "itemRef");
            case "remove_dataset_item":
                return List.of("datasetRef", "itemRef");
            case "add_dataset_action":
                return List.of("datasetRef", "label", "action");
            case "update_dataset_action":
            case "remove_dataset_action":
                return List.of("datasetRef", "actionRef");
            case "add_dataset_filter":
                return List.of("datasetRef", "label", "code", "formIdRef", "root", "prefix", "type");
            case "update_dataset_filter":
            case "remove_dataset_filter":
                return List.of("datasetRef", "filterRef");
            case "create_dashboard":
                return List.of("title");
            case "add_chart":
                return List.of("dashboardRef", "title", "type");
            case "create_screen":
                return List.of("title");
            case "add_screen_action":
                return List.of("screenRef", "label", "nextType");
            case "update_screen_action":
            case "remove_screen_action":
                return List.of("screenRef", "actionRef");
            case "update_screen":
                return List.of("screenRef");
            case "remove_screen":
                return List.of("screenRef");
            case "create_lambda":
                return List.of("name", "source");
            case "update_lambda":
            case "remove_lambda":
                return List.of("lambdaRef");
            case "add_lambda_binding":
                return List.of("lambdaRef", "binding");
            case "remove_lambda_binding":
                return List.of("lambdaRef", "bindingRef");
            case "create_email_template":
                return List.of("name", "subject", "content");
            case "update_email_template":
            case "remove_email_template":
                return List.of("emailTemplateRef");
            case "create_navi_group":
                return List.of("title");
            case "update_navi_group":
                return List.of("groupRef");
            case "remove_navi_group":
                return List.of("groupRef");
            case "add_navi_item":
                return List.of("groupRef", "title", "type");
            case "update_navi_item":
                return List.of("itemRef");
            case "remove_navi_item":
                return List.of("itemRef");
            default:
                return List.of();
        }
    }

    /**
     * Every {@code $step} reference must resolve to an action that survived, and it must be defined
     * before it is used, otherwise the executor would apply steps in the wrong order.
     */
    private static List<String> danglingReferences(ArrayNode actions) {
        List<String> problems = new ArrayList<>();
        Set<String> available = new HashSet<>();
        for (JsonNode action : actions) {
            JsonNode payload = action.get("payload");
            payload.fields().forEachRemaining(entry -> {
                for (String ref : references(entry.getValue(), entry.getKey())) {
                    if (!available.contains(ref)) {
                        problems.add(action.path("id").asText() + " -> $" + ref);
                    }
                }
            });
            available.add(action.path("id").asText());
        }
        return problems;
    }

    private static List<String> references(JsonNode value, String fieldName) {
        List<String> refs = new ArrayList<>();
        if (value == null) {
            return refs;
        }
        if (value.isTextual()) {
            String v = value.asText();
            if ((fieldName.endsWith("Ref") || "parent".equals(fieldName))
                    && v.startsWith("$") && v.length() > 1) {
                refs.add(v.substring(1));
            }
        } else if (value.isArray()) {
            for (JsonNode child : value) {
                refs.addAll(references(child, fieldName));
            }
        } else if (value.isObject()) {
            value.fields().forEachRemaining(entry ->
                    refs.addAll(references(entry.getValue(), entry.getKey())));
        }
        return refs;
    }

    private static boolean isBlank(JsonNode node) {
        return node == null
                || node.isNull()
                || node.isMissingNode()
                || (node.isTextual() && node.asText().isBlank());
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? "" : node.asText("").trim();
    }

    private static String humanise(String id) {
        String spaced = id.replace('_', ' ').trim();
        return spaced.isEmpty() ? "Step" : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}