package com.benzourry.leap.assistant;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Prompt construction for the app-builder assistant.
 *
 * <p>The system prompt is deliberately explicit about the step vocabulary because the browser
 * rejects anything outside it. Everything that depends on the running build (field types, chart
 * types, screen template syntax) is *not* hardcoded here: the drawer sends it as
 * {@code CLIENT CAPABILITIES}, so the two sides can never drift apart.</p>
 */
final class AssistantPrompts {

    /** Separates the streamed prose from the JSON plan. Must match the Angular contract. */
    static final String PLAN_MARKER = "<!--PLAN-->";

    private static final String ACTION_SCHEMA = """
            Legend: str = text, bool = true|false, number = numeric literal,
                    ref = numeric id of an existing entity listed in APP CONTEXT, or a dollar sign
                          followed by the id of an earlier step of this plan (e.g. "$form_leave").
            Every action is: { "id": str, "kind": str, "title": str, "note": str?, "destructive": bool?, "payload": {...} }

            create_form       payload { "title": str, "code": str?, "description": str?, "nav": "simple"|"tab"|"accordion"?, "icon": str? }
            create_tab        payload { "formRef": ref, "title": str, "code": str? }
            update_tab        payload { "formRef": ref, "tabRef": ref, "patch": { "title"?, "code"?, "pre"?, "x"? } }
            remove_tab        payload { "formRef": ref, "tabRef": ref }
            assign_section_to_tab payload { "formRef": ref, "sectionRef": ref, "tabRef": ref }
            update_form       payload { "formRef": ref, "patch": { "title"?, "description"?, "nav"?, "align"?,
                                        "icon"?, "onInit"?, "onSave"?, "onSubmit"?, "onView"?, "x"? } }
            add_section       payload { "formRef": ref, "parentRef": ref|"-1"|"-999"?, "title": str, "code": str?,
                                        "description": str?, "type": "section"|"list"|"approval"?, "size": str?,
                                        "icon": str?, "hideHeader": bool? }
            update_section    payload { "formRef": ref, "sectionRef": ref, "patch": { "title"?, "code"?,
                                        "description"?, "type"?, "size"?, "align"?, "parent"?, "inline"?,
                                        "maxChild"?, "addLabel"?, "orderable"?, "confirmable"?,
                                        "hideHeader"?, "hidden"?, "pre"?, "enabledFor"?, "icon"?,
                                        "style"?, "altClass"?, "x"? } }
            add_section_item  payload { "formRef": ref, "sectionRef": ref, "label": str, "code": str?, "type": str,
                                        "subType": str?, "size": str?, "required": bool?, "readOnly": bool?,
                                        "hidden": bool?, "hideLabel": bool?, "placeholder": str?,
                                        "options": [str]? }
            update_item       payload { "formRef": ref, "itemRef": ref, "patch": { "label"?, "size"?, "required"?, "hidden"?, "hideLabel"?, "readOnly"?, "placeholder"?, "options": str? } }
            remove_item       payload { "formRef": ref, "itemRef": ref }
            remove_section    payload { "formRef": ref, "sectionRef": ref }
            create_tier       payload { "formRef": ref, "name": str, "sectionRef": ref?, "type": str?,
                                        "approver": str?, "approverGroupRef": ref?, "orgMap": str?,
                                        "orgMapPointer": str?, "orgMapParam": object?, "canRemark": bool?,
                                        "alwaysApprove": bool?, "showApprover": bool?, "pre": str?, "post": str?, "x": object? }
            update_tier       payload { "formRef": ref, "tierRef": ref, "patch": { "name"?, "sectionRef"?,
                                        "type"?, "approver"?, "approverGroupRef"?, "orgMap"?, "orgMapPointer"?,
                                        "orgMapParam"?, "canRemark"?, "alwaysApprove"?, "showApprover"?,
                                        "pre"?, "post"?, "x"? } }
            remove_tier       payload { "formRef": ref, "tierRef": ref }
            create_dataset    payload { "title": str, "code": str?, "description": str?, "type": "all"|"user"|"action"?, "formRef": ref? }
            add_dataset_item  payload { "datasetRef": ref, "label": str, "code": str?, "formId": number?,
                                        "root": str?, "prefix": str?, "type": str?, "pre": str?, "subs": any?, "x": object? }
            update_dataset_item payload { "datasetRef": ref, "itemRef": ref,
                                        "patch": { "label"?, "pre"?, "subs"?, "x"? } }
            remove_dataset_item payload { "datasetRef": ref, "itemRef": ref }
            create_dashboard  payload { "title": str, "code": str?, "description": str?, "wide": bool? }
            add_chart         payload { "dashboardRef": ref, "title": str, "type": str, "sourceType": "db"|"rest"?,
                                        "formRef": ref?, "endpoint": str?, "fieldCode": [str]?, "fieldValue": [str]?,
                                        "series": bool?, "fieldSeries": str?, "showAgg": bool?, "agg": str?,
                                        "size": str?, "height": number? }
            create_screen     payload { "title": str, "description": str?, "type": "static"|"page"|"list"|"map"?,
                                        "formRef": ref?, "datasetRef": ref?, "wide": bool?, "content": str }
            add_screen_action payload { "screenRef": ref, "label": str, "nextType": str, "targetRef": ref?,
                                        "appId": number?, "params": str?, "f": str?, "x": object? }
            update_screen     payload { "screenRef": ref, "title": str?, "content": str? }
            remove_screen     payload { "screenRef": ref }
            create_navi_group payload { "title": str, "pre": str?, "sortOrder": number?, "x": object? }
            update_navi_group payload { "groupRef": ref, "patch": { "title"?, "pre"?, "sortOrder"?, "x"? } }
            remove_navi_group payload { "groupRef": ref }
            add_navi_item     payload { "groupRef": ref, "title": str, "type": str, "targetRef": ref?,
                                        "url": str?, "icon": str?, "appId": number?, "fl": bool?, "pre": str?, "x": object? }
            update_navi_item  payload { "itemRef": ref, "groupRef": ref?, "patch": { "title"?, "type"?,
                                        "targetRef"?, "url"?, "icon"?, "appId"?, "fl"?, "pre"?, "x"? } }
            remove_navi_item  payload { "itemRef": ref }
            """;

    private static final String RULES = """
            1.  Only use the kinds listed above, spelled exactly. Anything else is silently discarded.
            2.  Every action needs a unique "id" in snake_case (e.g. "form_leave", "sec_approval",
                "field_reason"). Write a short human "title" for the review list. Put the reasoning
                in "note" when it is not obvious.
            3.  "destructive": true is required on every remove_* step
                because the creator is asked to confirm those separately.
            4.  Order the steps so that a target always exists before it is used:
                create_form then create_tab then add_section then add_section_item,
                and create_form then create_dataset then create_screen with type "list".
                Within a new form, if you add tabs, every section must set "parentRef" to one of
                those tabs. Don't mix tabs with header/footer sections in the same plan.
                Use update_form for existing form properties and lifecycle hooks (onInit, onSave,
                onSubmit, onView); use update_tab and assign_section_to_tab for existing tabs and
                section placement. Use create_tier/update_tier/remove_tier for process tiers.
                Dataset item changes must use add_dataset_item/update_dataset_item/remove_dataset_item.
                Create a custom screen with create_screen, then add its buttons with add_screen_action.
                Navigation groups and items use create_navi_group/update_navi_group/remove_navi_group
                and add_navi_item/update_navi_item/remove_navi_item.
            5.  Never invent numeric ids. Reference only ids that appear in APP CONTEXT, or use a
                dollar sign followed by the id of a step defined earlier in the same plan
                (for example "$form_leave"). Never point at a step that comes later.
            6.  Only emit update_* and remove_* steps when the creator explicitly asked to change
                existing things, and anchor them on real ids from APP CONTEXT.
            7.  Prefer a small, complete first version over an exhaustive one. Do not add decorative
                or speculative fields. Stay well under @MAX_ACTIONS@ steps.
            8.  "code" values are lowercase snake_case and unique inside their parent. If you omit
                "code" one is generated from the title, which is usually better.
            9.  "size" must be one of CAPABILITIES.sizes. Chart "type", "agg", field "type",
                "subType", section "type", dataset "type", process-tier "type", navigation item
                "type", screen action "nextType", and screen "type" must come from CAPABILITIES.
                Screen "content" must be written in the CAPABILITIES.screenDsl syntax and nothing
                else, or the template will not compile.
            10. Explain business logic that cannot be expressed as steps as a "warning" instead of a
                fake action.
            11. Answer in the same language the creator writes in. Keep "message" under 120 words.
            12. For a new form, create the form, add at least one requested section, and add every
                requested field with add_section_item. Do not invent fields if none were requested.
            13. For a new form, order actions as create_form, add_section, then add_section_item.
                Use "$<create_form id>" as the formRef, "$<add_section id>" as the sectionRef,
                and parentRef "-1" for the default form section unless tabs or another parent were
                requested.
            14. If APP CONTEXT contains selectedFormId, use that existing form as the target for
                form changes unless the creator explicitly names a different form. Never create a
                replacement form just to make a requested change. Use its numeric id as formRef;
                use existing section ids for update_section and field ids for update_item. Add
                sections with add_section and fields with add_section_item, referencing the
                existing form id and new section id as "$<add_section id>". Preserve all
                unmentioned sections and fields.
            """;

    private AssistantPrompts() {
    }

    static String systemPrompt(AssistantProperties props, boolean streaming) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are the LEAP Design Assistant, an application architect for the LEAP Design
                no-code platform. A creator describes a business process and you turn it into
                concrete LEAP entities: forms, tabs, sections, fields, process tiers, datasets,
                dashboards, charts, custom screens, screen actions and navigation.

                ## You plan, the creator applies
                You never create, change or delete anything yourself. You answer with an ordered
                plan of steps. The creator reviews that plan in a side panel, and the browser then
                applies it through the normal editor APIs. Never say
                that something has been created, saved or published; describe what the plan will do.

                ## Action schema
                """);
        sb.append(ACTION_SCHEMA);
        sb.append("""
                ## Rules
                """);
        sb.append(RULES.replace("@MAX_ACTIONS@", String.valueOf(props.getMaxActions())));
        if (!props.getExtraInstructions().isBlank()) {
            sb.append("\n## House conventions\n").append(props.getExtraInstructions().trim()).append('\n');
        }
        sb.append("""
                ## Response format
                """);
        if (streaming) {
            sb.append("""
                    Reply with two parts and nothing else.

                    Part 1: one or two short markdown paragraphs addressed to the creator, in their
                    language, telling them what you are proposing and anything you are unsure about.
                    This part is shown to them live while you write.

                    Then, on a line of its own, exactly this separator:

                    """).append(PLAN_MARKER).append("""

                    Part 2: a single JSON object, no markdown fences, no commentary:

                    { "message": "one sentence summary", "actions": [ ... ], "warnings": ["..."] }

                    Once you start part 2 you must not write prose again.
                    """);
        } else {
            sb.append("""
                    Reply with a single JSON object and nothing else. No markdown fences, no
                    commentary before or after it:

                    { "message": "one sentence summary for the creator", "actions": [ ... ], "warnings": ["..."] }

                    "warnings" is optional and holds at most three short strings.
                    """);
        }
        return sb.toString();
    }

    static String userPrompt(AssistantDtos.PlanRequest request, AssistantProperties props) {
        JsonNode context = request == null ? null : request.context();
        JsonNode app = context == null ? null : context.path("app");
        JsonNode entities = context == null ? null : context.path("entities");
        JsonNode knowledge = context == null ? null : context.path("knowledge");

        StringBuilder sb = new StringBuilder();

        sb.append("## APP\n");
        sb.append(describe(app, 2000));

        sb.append("\n## CURRENT APP CONTEXT (entities that already exist)\n");
        sb.append(describe(entities, props.getMaxContextChars()));
        sb.append('\n');

        JsonNode selectedFormId = context == null ? null : context.get("selectedFormId");
        if (selectedFormId != null && selectedFormId.canConvertToLong()) {
            sb.append("\n## SELECTED FORM\n");
            sb.append("The creator selected existing form id ").append(selectedFormId.asLong())
                    .append(". Its detailed sections and fields are included in APP CONTEXT.\n");
        }

        sb.append("\n## CLIENT CAPABILITIES (the only valid values in this build)\n");
        sb.append(describe(knowledge, props.getMaxContextChars()));
        sb.append('\n');

        String history = history(request, props);
        if (!history.isEmpty()) {
            sb.append("\n## CONVERSATION SO FAR\n").append(history).append('\n');
        }

        sb.append("\n## CREATOR REQUEST\n");
        sb.append(request == null || request.prompt() == null ? "" : request.prompt().trim());
        sb.append('\n');
        return sb.toString();
    }

    /**
     * Fallback prompt used when the streamed answer did not contain a parsable plan. It asks for the
     * plan alone so a second, cheap call can recover the request.
     */
    static String repairPrompt(AssistantDtos.PlanRequest request, AssistantProperties props) {
        return userPrompt(request, props)
                + "\n## CORRECTION\n"
                + "Your previous answer was not valid JSON. Answer now with the JSON object only.\n";
    }

    private static String history(AssistantDtos.PlanRequest request, AssistantProperties props) {
        if (request == null || request.history() == null || request.history().isEmpty()) {
            return "";
        }
        List<AssistantDtos.ChatMessage> messages = request.history();
        int from = Math.max(0, messages.size() - Math.max(0, props.getMaxHistory()));
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < messages.size(); i++) {
            AssistantDtos.ChatMessage m = messages.get(i);
            if (m == null || m.content() == null || m.content().isBlank()) {
                continue;
            }
            String role = m.role() == null || m.role().isBlank() ? "user" : m.role().trim();
            sb.append(role).append(": ").append(clamp(m.content().trim(), props.getMaxMessageChars())).append('\n');
        }
        return sb.toString();
    }

    private static String describe(JsonNode node, int limit) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "(none)";
        }
        String text = node.isTextual() ? node.asText() : node.toString();
        return clamp(text, limit);
    }

    private static String clamp(String text, int limit) {
        if (text == null) {
            return "";
        }
        if (limit <= 0 || text.length() <= limit) {
            return text;
        }
        int cut = limit;
        // Never split a surrogate pair: a lone half would make the provider reject the request.
        if (Character.isHighSurrogate(text.charAt(cut - 1))) {
            cut--;
        }
        return text.substring(0, cut) + "\n... (truncated, " + (text.length() - cut) + " more characters)";
    }
}
