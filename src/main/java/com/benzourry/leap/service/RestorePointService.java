package com.benzourry.leap.service;

import com.benzourry.leap.exception.ExecutionException;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.RestorePoint;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.RestorePointRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//import java.awt.print.Pageable;

@Service
public class RestorePointService {

    private final RestorePointRepository restorePointRepository;

    private final AppRepository appRepository;

    private final AppService appService;

    @PersistenceContext
    private EntityManager entityManager;


    @Value("#{'${app.restorepoint.backup-db}'}")
    private String BACKUP_DB;

    @Value("#{'${app.restorepoint.active-db}'}")
    private String ACTIVE_DB;

//    private String[] TABLE_LIST_APP = new String[]{"app", "lookup", "lookup_entry", "user_group", "email_template",
//            "endpoint", "form", "tab", "item", "section", "section_item",
//            "tier", "tier_action", "dashboard", "chart", "chart_filter", "dataset",
//            "dataset_item", "dataset_filter", "screen", "action", "navi_group", "navi_item",
//            "schedule"};

    private final String[] TABLE_LIST_APP = new String[]{"app", "push_sub", "schedule", "user_group", "bucket", "endpoint", "email_template",
            "form", "tab", "item", "section", "section_item", "tier", "tier_action",
            "lookup", "lookup_entry", "navi_group", "navi_item", "dashboard", "chart", "chart_filter",
            "dataset", "dataset_item", "dataset_filter", "dataset_action", "screen", "action", "lambda", "lambda_bind"};


//    private final String[] TABLE_LIST_USERS = new String[]{"users", "app_user"};
//
//    private final String[] TABLE_LIST_ENTRY = new String[]{"entry", "entry_trail","entry_approval",
//            "entry_approval_trail", "entry_approver", "entry_attachment"};


    private final Map<String, String> COLUMN_NAME_MAP = Stream.of(new String[][]{
            {"app", "`id`, `app_path`, `clone`, `description`, `email`, `layout`, `logo`, `navi`, `public_access`, `secret`, `shared`, `status`, `tag`, `theme`, `title`, `use_email`, `use_facebook`, `use_github`, `use_google`, `use_linkedin`, `use_unimas`,`use_unimasid`,`use_icatsid`,`use_ssone`,`use_sarawakid`, `use_mydid`, `use_azuread`,`use_twitter`, `app_domain`, `block_anon`, `start_page`, `reg`, `use_anon`, `f`, `x`, `once`, `can_push`,`live`"},
            {"lookup", "`id`, `code_prop`, `data_enabled`, `desc_prop`, `description`, `email`, `endpoint`, `extra_prop`, `headers`, `json_root`, `name`, `response_type`, `method`, `shared`, `source_type`, `proxy_id`, `access`, `app`, `data_fields`, `auth`, `auth_flow`, `client_id`, `client_secret`, `token_endpoint`,`token_to`,`x`, `access_list`"},
            {"push_sub", "`endpoint`, `app_id`, `auth`, `p256dh`, `user_agent`, `user`, `timestamp`, `client` "},
            {"lookup_entry", "`id`, `code`, `data`, `enabled`, `extra`, `name`, `ordering`, `lookup` "},
            {"user_group", "`id`, `name`, `app`, `allow_reg`, `description`, `need_approval`, `tag_enabled`, `tag_ds`, `access_list` "},
            {"lambda", "`id`, `data`, `description`, `lang`, `access`, `app`,`name`,`email`,`public_access`,`scheduled`,`freq`,`clock`,`day_of_week`,`day_of_month`,`month_of_year`, `code` "},
            {"lambda_bind", "`id`, `name`, `type`, `src_id`, `params`, `lambda` "},
            {"bucket", "`id`, `app_id`, `app_name`, `code`, `description`, `email`, `name`, `timestamp`, `x`, `clock`, `day_of_month`, `day_of_week`, `freq`, `month_of_year`, `scheduled` "},
            {"cogna", "`id`, `clock`, `code`, `data`, `day_of_month`, `day_of_week`, `description`, `email`, `freq`, `model_path`, `model_type`, `month_of_year`, `name`, `public_access`, `scheduled`, `access`, `app`, `api_key`, `chunk_length`, `chunk_overlap`, `embed_model_name`, `embed_model_type`, `infer_model_name`, `infer_model_type`, `system_message`, `temperature`, `type`, `embed_model_api_key`, `infer_model_api_key`, `vector_store_dim`, `vector_store_host`, `vector_store_port`, `vector_store_type`, `embed_max_result`, `embed_min_score`, `max_chat_memory`, `max_token`, `post_message`, `augmentor`, `mm_support`"},
            {"cogna_source", "`id`, `name`, `params`, `src_id`, `type`, `cogna`, `sentence_tpl`, `src_url`, `last_ingest`, `clock`, `day_of_month`, `day_of_week`, `freq`, `month_of_year`, `scheduled`, `category_tpl`"},
            {"email_template", "`id`, `cc_admin`, `cc_approver`, `cc_user`, `content`, `creator`, `description`, `enabled`, `name`, `shared`, `subject`, `to_admin`, `to_approver`, `to_user`, `app`, `pickable`, `pushable`, `push_url`, `cc_extra`, `to_extra`, `log` "},
            {"endpoint", "`id`, `auth`, `auth_flow`, `client_id`, `client_secret`, `code`, `description`, `email`, `headers`, `json_root`, `method`, `name`, `response_type`, `shared`, `token_endpoint`,`token_to`, `url`, `app` "},
            {"form", "`id`, `admin`, `align`, `code_format`, `description`, `end_date`, `f`, `icon`, `inactive`, `nav`, `start_date`, `title`, `access_list`, `app`, `prev`, `counter`, `can_edit`, `can_retract`, `can_save`, `can_submit`, `validate_save`, `add_mailer`, `hide_status`, `on_save`, `on_submit`, `on_view`, `retract_mailer`, `single`, `single_q`, `update_appr_mailer`, `update_mailer`, `show_index`, `x`, `public_ep`, `sort_order` "},
            {"tab", "`id`, `code`, `pre`, `sort_order`, `title`, `form`, `x`"},
            {"item", "`id`, `bind_label`, `code`, `datasource`, `data_source_init`, `f`, `hidden`, `hide_label`, `hint`, `label`, `options`, `placeholder`, `post`, `pre`, `read_only`, `size`, `sub_type`, `type`, `v`, `form`, `format`, `x`, `facet` "},
            {"section", "`id`, `align`, `code`, `description`, `enabled_for`, `hide_header`, `main`, `parent`, `pre`, `size`, `sort_order`, `title`, `type`, `form`, `inline`, `style`, `max_child`, `icon`, `add_label`, `hidden`, `orderable`, `confirmable`, `for_approval`,`x` "},
            {"section_item", "`id`, `code`, `sort_order`, `section` "},
            {"tier", "`id`, `always_approve`, `approver`, `approver_grp`, `assign_mailer`, `can_approve`, `can_reject`, `can_remark`, `can_return`, `can_skip`, `name`, `org_map`, `org_map_pointer`, `resubmit_mailer`, `show_approver`, `sort_order`, `submit_mailer`, `type`, `form`, `form_section`, `org_map_param`, `post`, `pre`, `assigner` "},
            {"tier_action", "`id`, `action`, `code`, `color`, `icon`, `label`, `mailer`, `next_tier`, `pre`, `sort_order`, `user_edit`, `tier` "},
            {"dashboard", "`id`, `code`, `description`, `size`, `sort_order`, `title`, `type`, `access`, `app`, `wide`, `access_list`, `x` "},
            {"chart", "`id`, `agg`, `can_view`, `description`, `field_code`, `field_value`, `height`, `root_code`, `root_value`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `dashboard`, `form`, `preset_filters`, `field_series`, `root_series`, `series`, `show_agg`, `x`, `endpoint`, `f`, `source_type` "},
            {"chart_filter", "`id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `chart`, `prefix`, `type` "},
            {"dataset", "`id`, `code`, `description`, `export_csv`, `export_pdf`, `export_pdf_layout`, `export_xls`, `preset_filters`, `show_action`, `show_status`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `ui`, `ui_template`, `access_list`, `app`, `form`, `wide`, `blast_to`, `can_blast`, `default_sort`,`def_sort_field`,`def_sort_dir`, `show_index`, `public_ep`, `inpop` "},
            {"dataset_item", "`id`, `code`, `form_id`, `label`, `root`, `sort_order`, `dataset`, `type`, `prefix`, `pre`, `fields`, `subs` "},
            {"dataset_filter", "`id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `dataset`, `prefix`, `type` "},
            {"dataset_action", "`id`, `label`, `type`, `icon`, `style`, `sort_order`, `inpop`, `action`, `next`, `pre`, `f`, `url`, `params`, `dataset` "},
            {"screen", "`id`, `data`, `description`, `next`, `sort_order`, `title`, `type`, `app`, `dataset`, `form`, `can_print`, `wide`, `show_action`, `cogna`, `access_list`, `bucket` "},
            {"action", "`id`, `label`, `next`, `next_type`, `type`, `screen`, `params` "},
            {"navi_group", "`id`, `sort_order`, `title`, `access_list`, `pre`, `x`, `app` "},
            {"navi_item", "`id`, `fl`, `screen_id`, `sort_order`, `title`, `type`, `url`, `navi_group`, `icon`, `pre`, `x`, `app_id` "},
            {"schedule", "`id`, `clock`, `dataset_id`, `day_of_month`, `day_of_week`, `description`, `email`, `enabled`, `freq`, `mailer_id`, `name`, `type`, `app`, `month_of_year` "},
//            {"users", "`id`, `email`, `email_verified`, `image_url`, `name`, `password`, `provider`, `provider_id`, `provider_token`, `app_id`, `attributes`, `first_login`, `last_login`, `status`, `once` "},
//            {"app_user", "`id`, `status`, `user_group`, `user`, `sort_order` "},
//            {"entry", "`id`, `current_status`, `current_tier`, `current_tier_id`, `data`, `email`, `final_tier_id`, `prev_entry`, `resubmission_date`, `submission_date`, `form`, `created_by`, `created_date`, `modified_by`, `modified_date`, `deleted`, `current_edit`, `live` "},
//            {"entry_trail", "`id`, `snap`,`snap_tier`,`snap_tier_id`,`snap_status`,`snap_edit`, `email`, `entry_id`, `form_id`, `remark`, `action`, `timestamp` "},
//            {"entry_approval", "`id`, `data`, `email`, `remark`, `status`, `timestamp`, `entry`, `tier`, `tier_id`,`approver`,`created_by`, `created_date`, `modified_by`, `modified_date`, `deleted` "},
//            {"entry_approval_trail", "`id`, `data`, `email`, `entry_id`, `remark`, `status`, `timestamp`, `tier` "},
//            {"entry_approver", "`entry_id`, `approver`, `tier_id` "},
//            {"entry_attachment", "`id`, `file_name`, `file_size`, `file_type`, `file_url`, `item_id`, `message`, `success`, `timestamp`, `bucket_code`, `bucket_id`, `email`, `item_label`, `app_id`,`entry_id` "},
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    private final Map<String, String> BACKUP_SQL_MAP = Stream.of(new String[][]{
            {"app", "insert ignore into #BACKUP_DB#.app " +
                    "      (`id`, `app_path`, `clone`, `description`, `email`, `layout`, `logo`, `navi`, `public_access`, `secret`, `shared`, `status`, `tag`, `theme`, `title`, `use_email`, `use_facebook`, `use_github`, `use_google`, `use_linkedin`, `use_unimas`,`use_unimasid`,`use_icatsid`,`use_ssone`,`use_sarawakid`,`use_mydid`,`use_azuread`,`use_twitter`, `app_domain`, `block_anon`, `start_page`, `reg`, `use_anon`, `f`, `x`, `once`, `can_push`, `live`, `hash`) " +
                    "select `id`, `app_path`, `clone`, `description`, `email`, `layout`, `logo`, `navi`, `public_access`, `secret`, `shared`, `status`, `tag`, `theme`, `title`, `use_email`, `use_facebook`, `use_github`, `use_google`, `use_linkedin`, `use_unimas`,`use_unimasid`,`use_icatsid`,`use_ssone`,`use_sarawakid`,`use_mydid`,`use_azuread`,`use_twitter`, `app_domain`, `block_anon`, `start_page`, `reg`, `use_anon`, `f`, `x`, `once`, `can_push`, `live`, :hash from #ACTIVE_DB#.app " +
                    "where id = :appId"},
            {"push_sub", "insert ignore into #BACKUP_DB#.push_sub " +
                    "      (`endpoint`, `app_id`, `auth`, `p256dh`, `user_agent`, `user`, `timestamp`, `client`, `hash`)" +
                    "select `endpoint`, `app_id`, `auth`, `p256dh`, `user_agent`, `user`, `timestamp`, `client`, :hash from #ACTIVE_DB#.push_sub " +
                    "where app_id = :appId"},
            {"lookup", "insert ignore into #BACKUP_DB#.lookup " +
                    "      (`id`, `code_prop`, `data_enabled`, `desc_prop`, `description`, `email`, `endpoint`, `extra_prop`, `headers`, `json_root`, `name`, `response_type`, `method`, `shared`, `source_type`, `proxy_id`, `access`, `app`, `data_fields`, `auth`, `auth_flow`, `client_id`, `client_secret`, `token_endpoint`, `token_to`, `x`, `access_list`, `hash`)" +
                    "select `id`, `code_prop`, `data_enabled`, `desc_prop`, `description`, `email`, `endpoint`, `extra_prop`, `headers`, `json_root`, `name`, `response_type`, `method`, `shared`, `source_type`, `proxy_id`, `access`, `app`, `data_fields`, `auth`, `auth_flow`, `client_id`, `client_secret`, `token_endpoint`,`token_to`, `x`, `access_list`, :hash from #ACTIVE_DB#.lookup " +
                    "where app = :appId"},
            {"lookup_entry", "insert ignore into #BACKUP_DB#.lookup_entry " +
                    "      (`id`, `code`, `data`, `enabled`, `extra`, `name`, `ordering`, `lookup`, `hash`)" +
                    "select `id`, `code`, `data`, `enabled`, `extra`, `name`, `ordering`, `lookup`, :hash from #ACTIVE_DB#.lookup_entry " +
                    "where lookup in (select id from #ACTIVE_DB#.lookup where app = :appId)"},
            {"user_group", "insert ignore into #BACKUP_DB#.user_group " +
                    "      (`id`, `name`, `app`, `allow_reg`, `description`, `need_approval`, `tag_enabled`, `tag_ds`, `access_list`, `hash`) " +
                    "select `id`, `name`, `app`, `allow_reg`, `description`, `need_approval`, `tag_enabled`, `tag_ds`, `access_list`, :hash from #ACTIVE_DB#.user_group " +
                    "where app = :appId"},
            {"lambda", "insert ignore into #BACKUP_DB#.lambda " +
                    "      (`id`, `data`, `description`, `lang`, `access`, `app`,`name`,`email`,`public_access`, `scheduled`, `freq`,`clock`,`day_of_week`,`day_of_month`,`month_of_year`, `code`, `hash`) " +
                    "select `id`, `data`, `description`, `lang`, `access`, `app`,`name`,`email`,`public_access`, `scheduled`, `freq`,`clock`,`day_of_week`,`day_of_month`,`month_of_year`, `code`, :hash from #ACTIVE_DB#.lambda " +
                    "where app = :appId"},
            {"lambda_bind", "insert ignore into #BACKUP_DB#.lambda_bind " +
                    "      (`id`, `name`, `type`, `src_id`, `params`, `lambda`, `hash`) " +
                    "select `id`, `name`, `type`, `src_id`, `params`, `lambda`, :hash from #ACTIVE_DB#.lambda_bind " +
                    "where lambda in (select id from #ACTIVE_DB#.lambda where app = :appId)"},
            {"bucket", "insert ignore into #BACKUP_DB#.bucket " +
                    "      (`id`, `app_id`, `app_name`, `code`, `description`, `email`, `name`, `timestamp`, `x`, `clock`, `day_of_month`, `day_of_week`, `freq`, `month_of_year`, `scheduled`, `hash`) " +
                    "select `id`, `app_id`, `app_name`, `code`, `description`, `email`, `name`, `timestamp`, `x`, `clock`, `day_of_month`, `day_of_week`, `freq`, `month_of_year`, `scheduled`, :hash from #ACTIVE_DB#.bucket " +
                    "where app_id = :appId"},
            {"cogna", "insert ignore into #BACKUP_DB#.cogna " +
                    "      (`id`, `clock`, `code`, `data`, `day_of_month`, `day_of_week`, `description`, `email`, `freq`, `model_path`, `model_type`, `month_of_year`, `name`, `public_access`, `scheduled`, `access`, `app`, `api_key`, `chunk_length`, `chunk_overlap`, `embed_model_name`, `embed_model_type`, `infer_model_name`, `infer_model_type`, `system_message`, `temperature`, `type`, `embed_model_api_key`, `infer_model_api_key`, `vector_store_dim`, `vector_store_host`, `vector_store_port`, `vector_store_type`, `embed_max_result`, `embed_min_score`, `max_chat_memory`, `max_token`, `post_message`, `augmentor`, `mm_support`, `hash`) " +
                    "select `id`, `clock`, `code`, `data`, `day_of_month`, `day_of_week`, `description`, `email`, `freq`, `model_path`, `model_type`, `month_of_year`, `name`, `public_access`, `scheduled`, `access`, `app`, `api_key`, `chunk_length`, `chunk_overlap`, `embed_model_name`, `embed_model_type`, `infer_model_name`, `infer_model_type`, `system_message`, `temperature`, `type`, `embed_model_api_key`, `infer_model_api_key`, `vector_store_dim`, `vector_store_host`, `vector_store_port`, `vector_store_type`, `embed_max_result`, `embed_min_score`, `max_chat_memory`, `max_token`, `post_message`, `augmentor`, `mm_support`, :hash from #ACTIVE_DB#.bucket " +
                    "where app = :appId"},
            {"cogna_source", "insert ignore into #BACKUP_DB#.cogna " +
                    "      (`id`, `name`, `params`, `src_id`, `type`, `cogna`, `sentence_tpl`, `src_url`, `last_ingest`, `clock`, `day_of_month`, `day_of_week`, `freq`, `month_of_year`, `scheduled`, `category_tpl`, `hash`) " +
                    "select `id`, `name`, `params`, `src_id`, `type`, `cogna`, `sentence_tpl`, `src_url`, `last_ingest`, `clock`, `day_of_month`, `day_of_week`, `freq`, `month_of_year`, `scheduled`, `category_tpl`, :hash from #ACTIVE_DB#.bucket " +
                    "where cogna in (select id from #ACTIVE_DB#.cogna " +
                    "where app = :appId"},
            {"email_template", "insert ignore into #BACKUP_DB#.email_template " +
                    "      (`id`, `cc_admin`, `cc_approver`, `cc_user`, `content`, `creator`, `description`, `enabled`, `name`, `shared`, `subject`, `to_admin`, `to_approver`, `to_user`, `app`, `pickable`, `pushable`, `push_url`, `cc_extra`, `to_extra`, `log`, `hash`) " +
                    "select `id`, `cc_admin`, `cc_approver`, `cc_user`, `content`, `creator`, `description`, `enabled`, `name`, `shared`, `subject`, `to_admin`, `to_approver`, `to_user`, `app`, `pickable`, `pushable`, `push_url`, `cc_extra`, `to_extra`, `log`, :hash from #ACTIVE_DB#.email_template " +
                    "where app = :appId"},
            {"endpoint", "insert ignore into #BACKUP_DB#.endpoint " +
                    "      (`id`, `auth`, `auth_flow`, `client_id`, `client_secret`, `code`, `description`, `email`, `headers`, `json_root`, `method`, `name`, `response_type`, `shared`, `token_endpoint`, `token_to`, `url`, `app`, `hash`) " +
                    "select `id`, `auth`, `auth_flow`, `client_id`, `client_secret`, `code`, `description`, `email`, `headers`, `json_root`, `method`, `name`, `response_type`, `shared`, `token_endpoint`, `token_to`, `url`, `app`, :hash from #ACTIVE_DB#.endpoint " +
                    "where app = :appId"},
            {"form", "insert ignore into #BACKUP_DB#.form " +
                    "      (`id`, `admin`, `align`, `code_format`, `description`, `end_date`, `f`, `icon`, `inactive`, `nav`, `start_date`, `title`, `access_list`, `app`, `prev`, `counter`, `can_edit`, `can_retract`, `can_save`, `can_submit`, `validate_save`, `add_mailer`, `hide_status`, `on_save`, `on_submit`, `on_view`, `retract_mailer`, `single`, `single_q`, `update_appr_mailer`, `update_mailer`, `show_index`, `x`, `public_ep`, `sort_order`, `hash`) " +
                    "select `id`, `admin`, `align`, `code_format`, `description`, `end_date`, `f`, `icon`, `inactive`, `nav`, `start_date`, `title`, `access_list`, `app`, `prev`, `counter`, `can_edit`, `can_retract`, `can_save`, `can_submit`, `validate_save`, `add_mailer`, `hide_status`, `on_save`, `on_submit`, `on_view`, `retract_mailer`, `single`, `single_q`, `update_appr_mailer`, `update_mailer`, `show_index`, `x`, `public_ep`, `sort_order`, :hash from #ACTIVE_DB#.form " +
                    "where app = :appId"},
            {"tab", "insert ignore into #BACKUP_DB#.tab " +
                    "      (`id`, `code`, `pre`, `sort_order`, `title`, `form`, `x`, `hash`) " +
                    "select `id`, `code`, `pre`, `sort_order`, `title`, `form`, `x`, :hash from #ACTIVE_DB#.tab " +
                    "where form in (select id from #ACTIVE_DB#.form " +
                    "where app = :appId)"},
            {"item", "insert ignore into #BACKUP_DB#.item " +
                    "      (`id`, `bind_label`, `code`, `datasource`, `data_source_init`, `f`, `hidden`, `hide_label`, `hint`, `label`, `options`, `placeholder`, `post`, `pre`, `read_only`, `size`, `sub_type`, `type`, `v`, `form`, `format`, `x`, `facet`, `hash`)" +
                    "select `id`, `bind_label`, `code`, `datasource`, `data_source_init`, `f`, `hidden`, `hide_label`, `hint`, `label`, `options`, `placeholder`, `post`, `pre`, `read_only`, `size`, `sub_type`, `type`, `v`, `form`, `format`, `x`, `facet`, :hash from #ACTIVE_DB#.item " +
                    "where form in (select id from #ACTIVE_DB#.form " +
                    "where app = :appId)"},
            {"section", "insert ignore into #BACKUP_DB#.section " +
                    "      (`id`, `align`, `code`, `description`, `enabled_for`, `hide_header`, `main`, `parent`, `pre`, `size`, `sort_order`, `title`, `type`, `form`, `inline`, `style`, `max_child`, `icon`, `add_label`,`hidden`, `orderable`, `confirmable`, `for_approval`,`x`, `hash`) " +
                    "select `id`, `align`, `code`, `description`, `enabled_for`, `hide_header`, `main`, `parent`, `pre`, `size`, `sort_order`, `title`, `type`, `form`, `inline`, `style`, `max_child`, `icon`, `add_label`,`hidden`, `orderable`, `confirmable`, `for_approval`,`x`, :hash from #ACTIVE_DB#.section " +
                    "where form in (select id from #ACTIVE_DB#.form " +
                    "where app = :appId)"},
            {"section_item", "insert ignore into #BACKUP_DB#.section_item " +
                    "      (`id`, `code`, `sort_order`, `section`, `hash`) " +
                    "select `id`, `code`, `sort_order`, `section`, :hash from #ACTIVE_DB#.section_item " +
                    "where section in (select id from #ACTIVE_DB#.section " +
                    "where form in (select id from #ACTIVE_DB#.form " +
                    "where app = :appId))"},
            {"tier", "insert ignore into #BACKUP_DB#.tier " +
                    "      (`id`, `always_approve`, `approver`,  `approver_grp`, `assign_mailer`, `can_approve`, `can_reject`, `can_remark`, `can_return`, `can_skip`, `name`, `org_map`, `org_map_pointer`, `resubmit_mailer`, `show_approver`, `sort_order`, `submit_mailer`, `type`, `form`, `form_section`, `org_map_param`, `post`, `pre`, `assigner`, `hash`) " +
                    "select `id`, `always_approve`, `approver`,  `approver_grp`, `assign_mailer`, `can_approve`, `can_reject`, `can_remark`, `can_return`, `can_skip`, `name`, `org_map`, `org_map_pointer`, `resubmit_mailer`, `show_approver`, `sort_order`, `submit_mailer`, `type`, `form`, `form_section`, `org_map_param`, `post`, `pre`, `assigner`, :hash from #ACTIVE_DB#.tier " +
                    "where form in (select id from #ACTIVE_DB#.form " +
                    "where app = :appId)"},
            {"tier_action", "insert ignore into #BACKUP_DB#.tier_action " +
                    "      (`id`, `action`, `code`, `color`, `icon`, `label`, `mailer`, `next_tier`, `pre`, `sort_order`, `user_edit`, `tier`, `hash`) " +
                    "select `id`, `action`, `code`, `color`, `icon`, `label`, `mailer`, `next_tier`, `pre`, `sort_order`, `user_edit`, `tier`, :hash from #ACTIVE_DB#.tier_action " +
                    "where tier in (select id from #ACTIVE_DB#.tier " +
                    "where form in (select id from #ACTIVE_DB#.form " +
                    "where app = :appId))"},
            {"dashboard", "insert ignore into #BACKUP_DB#.dashboard " +
                    "      (`id`, `code`, `description`, `size`, `sort_order`, `title`, `type`, `access`, `app`, `wide`, `access_list`, `x`, `hash`) " +
                    "select `id`, `code`, `description`, `size`, `sort_order`, `title`, `type`, `access`, `app`, `wide`, `access_list`, `x`, :hash from #ACTIVE_DB#.dashboard " +
                    "where app = :appId"},
            {"chart", "insert ignore into #BACKUP_DB#.chart " +
                    "      (`id`, `agg`, `can_view`, `description`, `field_code`, `field_value`, `height`, `root_code`, `root_value`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `dashboard`, `form`, `preset_filters`, `field_series`, `root_series`, `series`, `show_agg`, `x`, `endpoint`, `f`, `source_type`, `hash`) " +
                    "select `id`, `agg`, `can_view`, `description`, `field_code`, `field_value`, `height`, `root_code`, `root_value`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `dashboard`, `form`, `preset_filters`, `field_series`, `root_series`, `series`, `show_agg`, `x`, `endpoint`, `f`, `source_type`, :hash from #ACTIVE_DB#.chart " +
                    "where dashboard in (select id from #ACTIVE_DB#.dashboard " +
                    "where app = :appId)"},
            {"chart_filter", "insert ignore into #BACKUP_DB#.chart_filter" +
                    "      (`id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `chart`, `prefix`, `type`,`hash`) " +
                    "select `id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `chart`, `prefix`, `type`,:hash from #ACTIVE_DB#.chart_filter " +
                    "where chart in (select id from #ACTIVE_DB#.chart " +
                    "where dashboard in (select id from #ACTIVE_DB#.dashboard " +
                    "where app = :appId))"},
            {"dataset", "insert ignore into #BACKUP_DB#.dataset " +
                    "      (`id`, `code`, `description`, `export_csv`, `export_pdf`, `export_pdf_layout`, `export_xls`, `preset_filters`, `show_action`, `show_status`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `ui`, `ui_template`, `access_list`, `app`, `form`, `wide`, `blast_to`, `can_blast`, `default_sort`,`def_sort_field`,`def_sort_dir`, `show_index`, `public_ep`,`inpop`,`x`, `hash`) " +
                    "select `id`, `code`, `description`, `export_csv`, `export_pdf`, `export_pdf_layout`, `export_xls`, `preset_filters`, `show_action`, `show_status`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `ui`, `ui_template`, `access_list`, `app`, `form`, `wide`, `blast_to`, `can_blast`, `default_sort`,`def_sort_field`,`def_sort_dir`, `show_index`, `public_ep`,`inpop`,`x`, :hash from #ACTIVE_DB#.dataset " +
                    "where app = :appId"},
            {"dataset_item", "insert ignore into #BACKUP_DB#.dataset_item " +
                    "      (`id`, `code`, `form_id`, `label`, `root`, `sort_order`, `dataset`, `type`, `prefix`, `pre`, `fields`, `subs`, `hash`) " +
                    "select `id`, `code`, `form_id`, `label`, `root`, `sort_order`, `dataset`, `type`, `prefix`, `pre`, `fields`, `subs`, :hash from #ACTIVE_DB#.dataset_item " +
                    "where dataset in (select id from #ACTIVE_DB#.dataset " +
                    "where app = :appId)"},
            {"dataset_filter", "insert ignore into #BACKUP_DB#.dataset_filter " +
                    "      (`id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `dataset`, `prefix`, `type`, `hash`) " +
                    "select `id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `dataset`, `prefix`, `type`, :hash from #ACTIVE_DB#.dataset_filter " +
                    "where dataset in (select id from #ACTIVE_DB#.dataset " +
                    "where app = :appId)"},
            {"dataset_action", "insert ignore into #BACKUP_DB#.dataset_action " +
                    "      (`id`, `label`, `type`, `icon`, `style`, `sort_order`, `inpop`, `action`, `next`, `pre`, `f`,`url`, `params`,`dataset`, `hash`) " +
                    "select `id`, `label`, `type`, `icon`, `style`, `sort_order`, `inpop`, `action`, `next`, `pre`, `f`,`url`, `params`,`dataset`, :hash from #ACTIVE_DB#.dataset_action " +
                    "where dataset in (select id from #ACTIVE_DB#.dataset " +
                    "where app = :appId)"},
            {"screen", "insert ignore into #BACKUP_DB#.screen " +
                    "      (`id`, `data`, `description`, `next`, `sort_order`, `title`, `type`, `app`, `dataset`, `form`, `can_print`, `wide`, `show_action`,`cogna`, `bucket`, `access_list`, `hash`) " +
                    "select `id`, `data`, `description`, `next`, `sort_order`, `title`, `type`, `app`, `dataset`, `form`, `can_print`, `wide`, `show_action`,`cogna`, `bucket`, `access_list`, :hash from #ACTIVE_DB#.screen " +
                    "where app = :appId"},
            {"action", "insert ignore into #BACKUP_DB#.action " +
                    "      (`id`, `label`, `next`, `next_type`, `type`, `screen`, `params`, `hash`) " +
                    "select `id`, `label`, `next`, `next_type`, `type`, `screen`, `params`, :hash from #ACTIVE_DB#.action " +
                    "where screen in (select id from #ACTIVE_DB#.screen " +
                    "where app = :appId)"},
            {"navi_group", "insert ignore into #BACKUP_DB#.navi_group " +
                    "      (`id`, `sort_order`, `title`, `access_list`, `pre`, `x`, `app`, `hash`) " +
                    "select `id`, `sort_order`, `title`, `access_list`, `pre`, `x`, `app`, :hash from #ACTIVE_DB#.navi_group " +
                    "where app = :appId"},
            {"navi_item", "insert ignore into #BACKUP_DB#.navi_item " +
                    "      (`id`, `fl`, `screen_id`, `sort_order`, `title`, `type`, `url`, `navi_group`, `icon`, `pre`, `x`, `app_id`, `hash`) " +
                    "select `id`, `fl`, `screen_id`, `sort_order`, `title`, `type`, `url`, `navi_group`, `icon`, `pre`, `x`, `app_id`, :hash from #ACTIVE_DB#.navi_item " +
                    "where navi_group in (select id from #ACTIVE_DB#.navi_group " +
                    "where app = :appId)"},
            {"schedule", "insert ignore into #BACKUP_DB#.schedule " +
                    "      (`id`, `clock`, `dataset_id`, `day_of_month`, `day_of_week`, `description`, `email`, `enabled`, `freq`, `mailer_id`, `name`, `type`, `app`, `month_of_year`, `hash`) " +
                    "select `id`, `clock`, `dataset_id`, `day_of_month`, `day_of_week`, `description`, `email`, `enabled`, `freq`, `mailer_id`, `name`, `type`, `app`, `month_of_year`, :hash from #ACTIVE_DB#.schedule " +
                    "where app = :appId"},
//            {"users", "insert ignore into #BACKUP_DB#.users " +
//                    "      (`id`, `email`, `email_verified`, `image_url`, `name`, `password`, `provider`, `provider_id`, `provider_token`, `app_id`, `attributes`, `first_login`, `last_login`, `status`, `once`, `hash`) " +
//                    "select `id`, `email`, `email_verified`, `image_url`, `name`, `password`, `provider`, `provider_id`, `provider_token`, `app_id`, `attributes`, `first_login`, `last_login`, `status`, `once`, :hash from #ACTIVE_DB#.users " +
//                    "where app_id = :appId"},
//            {"app_user", "insert ignore into #BACKUP_DB#.app_user " +
//                    "      (`id`, `status`, `user_group`, `user`, `sort_order`, `hash`) " +
//                    "select `id`, `status`, `user_group`, `user`, `sort_order`, :hash from #ACTIVE_DB#.app_user " +
//                    "where user in (select id from #ACTIVE_DB#.users " +
//                    "where app_id = :appId)"},
//            {"entry", "insert ignore into #BACKUP_DB#.entry " +
//                    "      (`id`, `current_status`, `current_tier`, `current_tier_id`, `data`, `email`, `final_tier_id`, `prev_entry`, `resubmission_date`, `submission_date`, `form`, `created_by`, `created_date`, `modified_by`, `modified_date`, `deleted`, `current_edit`, `live`, `hash`) " +
//                    "select `id`, `current_status`, `current_tier`, `current_tier_id`, `data`, `email`, `final_tier_id`, `prev_entry`, `resubmission_date`, `submission_date`, `form`, `created_by`, `created_date`, `modified_by`, `modified_date`, `deleted`, `current_edit`, `live`, :hash from #ACTIVE_DB#.entry " +
//                    "where form in (select id from #ACTIVE_DB#.form " +
//                    "where app = :appId)"},
//            {"entry_trail", "insert ignore into #BACKUP_DB#.entry_trail " +
//                    "      (`id`, `snap`, `email`, `entry_id`, `form_id`, `remark`, `action`, `timestamp`,`snap_tier`,`snap_tier_id`,`snap_status`,`snap_edit`, `hash`) " +
//                    "select `id`, `snap`, `email`, `entry_id`, `form_id`, `remark`, `action`, `timestamp`,`snap_tier`,`snap_tier_id`,`snap_status`,`snap_edit`, :hash from #ACTIVE_DB#.entry_trail " +
//                    "where form_id in (select id from #ACTIVE_DB#.form " +
//                    "where app = :appId)"},
//            {"entry_approval", "insert ignore into #BACKUP_DB#.entry_approval" +
//                    "      (`id`, `data`, `email`, `remark`, `status`, `timestamp`, `entry`, `tier`, `tier_id`,`approver`, `created_by`, `created_date`, `modified_by`, `deleted`, `modified_date`, `hash`) " +
//                    "select `id`, `data`, `email`, `remark`, `status`, `timestamp`, `entry`, `tier`, `tier_id`,`approver`, `created_by`, `created_date`, `modified_by`, `deleted`, `modified_date`, :hash from #ACTIVE_DB#.entry_approval " +
//                    "where entry in (select id from #ACTIVE_DB#.entry " +
//                    "where form in (select id from #ACTIVE_DB#.form " +
//                    "where app = :appId))"},
//            {"entry_approval_trail", "insert ignore into #BACKUP_DB#.entry_approval_trail " +
//                    "      (`id`, `data`, `email`, `entry_id`, `remark`, `status`, `timestamp`, `tier`, `hash`) " +
//                    "select `id`, `data`, `email`, `entry_id`, `remark`, `status`, `timestamp`, `tier`, :hash from #ACTIVE_DB#.entry_approval_trail " +
//                    "where entry_id in (select id from #ACTIVE_DB#.entry " +
//                    "where form in (select id from #ACTIVE_DB#.form " +
//                    "where app = :appId))"},
//            {"entry_approver", "insert ignore into #BACKUP_DB#.entry_approver " +
//                    "      (`entry_id`, `approver`, `tier_id`, `hash`) " +
//                    "select `entry_id`, `approver`, `tier_id`, :hash from #ACTIVE_DB#.entry_approver " +
//                    "where entry_id in (select id from #ACTIVE_DB#.entry " +
//                    "where form in (select id from #ACTIVE_DB#.form " +
//                    "where app = :appId))"},
//            {"entry_attachment", "insert ignore into #BACKUP_DB#.entry_attachment " +
//                    "      (`id`, `file_name`, `file_size`, `file_type`, `file_url`, `item_id`, `message`, `success`, `timestamp`, `bucket_code`, `bucket_id`, `email`, `item_label`, `app_id`,`entry_id`, `hash`) " +
//                    "select `id`, `file_name`, `file_size`, `file_type`, `file_url`, `item_id`, `message`, `success`, `timestamp`, `bucket_code`, `bucket_id`, `email`, `item_label`, `app_id`,`entry_id`, :hash from #ACTIVE_DB#.entry_attachment " +
//                    "where item_id in (select id from #ACTIVE_DB#.item " +
//                    "where form in (select id from #ACTIVE_DB#.form " +
//                    "where app = :appId))"}
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));


    private final Map<String, String> DELETE_SQL_MAP = Stream.of(new String[][]{
            {"app", "delete from #ACTIVE_DB#.app " +
                    "where id = :appId"},
            {"push_sub", "delete from #ACTIVE_DB#.push_sub " +
                    "where app_id = :appId"},
            {"lookup", "delete from #ACTIVE_DB#.lookup " +
                    "where app = :appId"},
            {"lookup_entry", "delete from #ACTIVE_DB#.lookup_entry " +
                    "where lookup in (select id from #ACTIVE_DB#.lookup where app = :appId)"},
            {"user_group", "delete from #ACTIVE_DB#.user_group " +
                    "where app = :appId"},
            {"lambda", "delete from #ACTIVE_DB#.lambda " +
                    "where app = :appId"},
            {"bucket", "delete from #ACTIVE_DB#.bucket " +
                    "where app_id = :appId"},
            {"email_template", "delete from #ACTIVE_DB#.email_template " +
                    "where app = :appId"},
            {"endpoint", "delete from #ACTIVE_DB#.endpoint " +
                    "where app = :appId"},
            {"form", "delete from #ACTIVE_DB#.form " +
                    "where app = :appId"},
            {"tab", "delete from #ACTIVE_DB#.tab " +
                    "where form in (select id from #ACTIVE_DB#.form " +
                    "where app = :appId)"},
            {"item", "delete from #ACTIVE_DB#.item " +
                    "where form in (select id from #ACTIVE_DB#.form " +
                    "where app = :appId)"},
            {"section", "delete from #ACTIVE_DB#.section " +
                    "where form in (select id from #ACTIVE_DB#.form " +
                    "where app = :appId)"},
            {"section_item", "delete from #ACTIVE_DB#.section_item " +
                    "where section in (select id from #ACTIVE_DB#.section " +
                    "where form in (select id from #ACTIVE_DB#.form " +
                    "where app = :appId))"},
            {"tier", "delete from #ACTIVE_DB#.tier " +
                    "where form in (select id from #ACTIVE_DB#.form " +
                    "where app = :appId)"},
            {"tier_action", "delete from #ACTIVE_DB#.tier_action " +
                    "where tier in (select id from #ACTIVE_DB#.tier " +
                    "where form in (select id from #ACTIVE_DB#.form " +
                    "where app = :appId))"},
            {"dashboard", "delete from #ACTIVE_DB#.dashboard " +
                    "where app = :appId"},
            {"chart", "delete from #ACTIVE_DB#.chart " +
                    "where dashboard in (select id from #ACTIVE_DB#.dashboard " +
                    "where app = :appId)"},
            {"chart_filter", "delete from #ACTIVE_DB#.chart_filter " +
                    "where chart in (select id from #ACTIVE_DB#.chart " +
                    "where dashboard in (select id from #ACTIVE_DB#.dashboard " +
                    "where app = :appId))"},
            {"dataset", "delete from #ACTIVE_DB#.dataset " +
                    "where app = :appId"},
            {"dataset_item", "delete from #ACTIVE_DB#.dataset_item " +
                    "where dataset in (select id from #ACTIVE_DB#.dataset " +
                    "where app = :appId)"},
            {"dataset_filter", "delete from #ACTIVE_DB#.dataset_filter " +
                    "where dataset in (select id from #ACTIVE_DB#.dataset " +
                    "where app = :appId)"},
            {"dataset_action", "delete from #ACTIVE_DB#.dataset_action " +
                    "where dataset in (select id from #ACTIVE_DB#.dataset " +
                    "where app = :appId)"},
            {"screen", "delete from #ACTIVE_DB#.screen " +
                    "where app = :appId"},
            {"action", "delete from #ACTIVE_DB#.action " +
                    "where screen in (select id from #ACTIVE_DB#.screen " +
                    "where app = :appId)"},
            {"navi_group", "delete from #ACTIVE_DB#.navi_group " +
                    "where app = :appId"},
            {"navi_item", "delete from #ACTIVE_DB#.navi_item " +
                    "where navi_group in (select id from #ACTIVE_DB#.navi_group " +
                    "where app = :appId)"},
            {"schedule", "delete from #ACTIVE_DB#.schedule " +
                    "where app = :appId"},
//            {"users", "delete from #ACTIVE_DB#.users " +
//                    "where app_id = :appId"},
//            {"app_user", "delete from #ACTIVE_DB#.app_user " +
//                    "where user in (select id from #ACTIVE_DB#.users " +
//                    "where app_id = :appId)"},
//            {"entry", "delete from #ACTIVE_DB#.entry " +
//                    "where form in (select id from #ACTIVE_DB#.form " +
//                    "where app = :appId)"},
//            {"entry_trail", "delete from #ACTIVE_DB#.entry_trail " +
//                    "where form_id in (select id from #ACTIVE_DB#.form " +
//                    "where app = :appId)"},
//            {"entry_approval", "delete from #ACTIVE_DB#.entry_approval " +
//                    "where entry in (select id from #ACTIVE_DB#.entry " +
//                    "where form in (select id from #ACTIVE_DB#.form " +
//                    "where app = :appId))"},
//            {"entry_approval_trail", "delete from #ACTIVE_DB#.entry_approval_trail " +
//                    "where entry_id in (select id from #ACTIVE_DB#.entry " +
//                    "where form in (select id from #ACTIVE_DB#.form " +
//                    "where app = :appId))"},
//            {"entry_approver", "delete from #ACTIVE_DB#.entry_approver " +
//                    "where entry_id in (select id from #ACTIVE_DB#.entry " +
//                    "where form in (select id from #ACTIVE_DB#.form " +
//                    "where app = :appId))"},
//            {"entry_attachment", "delete from #ACTIVE_DB#.entry_attachment " +
//                    "where item_id in (select id from #ACTIVE_DB#.item " +
//                    "where form in (select id from #ACTIVE_DB#.form " +
//                    "where app = :appId))"}
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));


    public RestorePointService(RestorePointRepository restorePointRepository, AppRepository appRepository, AppService appService) {
        this.restorePointRepository = restorePointRepository;
        this.appRepository = appRepository;
        this.appService = appService;
    }

    @Transactional
    public RestorePoint create(RestorePoint restorePoint, Long appId, String email) {

        ObjectMapper mapper = new ObjectMapper();
        String hash = DigestUtils.md5Hex(Instant.now().getEpochSecond() + ":" + appId).toUpperCase();
        Map<String, Integer> summary = new HashMap<>();
        if (restorePoint.isIncludeApp()) {

            Arrays.stream(this.TABLE_LIST_APP).forEach(tableName -> {
                try {
                    summary.put(tableName, this.entityManager.createNativeQuery(BACKUP_SQL_MAP.get(tableName)
                                    .replaceAll("#BACKUP_DB#", BACKUP_DB)
                                    .replaceAll("#ACTIVE_DB#", ACTIVE_DB)
                            )
                            .setParameter("hash", hash)
                            .setParameter("appId", appId)
                            .executeUpdate());
                } catch (Exception e) {
                    System.out.println("TableName:"+tableName+", Error:" + e.getMessage());
                    throw new ExecutionException("TableName:"+tableName+", Error:" + e.getMessage());
                }
            });
        }
//        if (restorePoint.isIncludeUsers()) {
//            Arrays.stream(this.TABLE_LIST_USERS).forEach(tableName -> {
//                try {
//                    summary.put(tableName, this.entityManager.createNativeQuery(BACKUP_SQL_MAP.get(tableName)
//                                    .replaceAll("#BACKUP_DB#", BACKUP_DB)
//                                    .replaceAll("#ACTIVE_DB#", ACTIVE_DB)
//                            )
//                            .setParameter("hash", hash)
//                            .setParameter("appId", appId)
//                            .executeUpdate());
//                } catch (Exception e) {
//                    System.out.println("TableName:"+tableName+", Error:" + e.getMessage());
//                    throw new ExecutionException("TableName:"+tableName+", Error:" + e.getMessage());
//                }
////            summary.put("users", restorePointRepository.backupUsers(appId, hash));
////            summary.put("app_user", restorePointRepository.backupAppUsers(appId, hash));
//            });
//        }
//        if (restorePoint.isIncludeEntry()) {
//            Arrays.stream(this.TABLE_LIST_ENTRY).forEach(tableName -> {
//                try {
//                    summary.put(tableName, this.entityManager.createNativeQuery(BACKUP_SQL_MAP.get(tableName)
//                        .replaceAll("#BACKUP_DB#", BACKUP_DB)
//                        .replaceAll("#ACTIVE_DB#", ACTIVE_DB)
//                    )
//                    .setParameter("hash", hash)
//                    .setParameter("appId", appId)
//                    .executeUpdate());
//                } catch (Exception e) {
//                    System.out.println("TableName:"+tableName+", Error:" + e.getMessage());
//                    throw new ExecutionException("TableName:"+tableName+", Error:" + e.getMessage());
//              }
//            });
////            summary.put("entry", restorePointRepository.backupEntry(appId, hash));
////            summary.put("entry_approval", restorePointRepository.backupEntryApproval(appId, hash));
////            summary.put("entry_approval_trail", restorePointRepository.backupEntryApprovalTrail(appId, hash));
////            summary.put("entry_approver", restorePointRepository.backupEntryApprover(appId, hash));
////            summary.put("entry_attachment", restorePointRepository.backupEntryAttachment(appId, hash));
//        }

        restorePoint.setTimestamp(new Date());
        restorePoint.setEmail(email);
        restorePoint.setHash(hash);

        restorePoint.setSummary(mapper.valueToTree(summary));
        App app = appRepository.getReferenceById(appId);
        restorePoint.setAppId(appId);
        restorePoint.setAppName(app.getTitle());
        return restorePointRepository.save(restorePoint);
    }

    public Page<RestorePoint> findByAppId(Long appId, Pageable pageable) {
        return restorePointRepository.findByAppId(appId, pageable);
    }

    @Transactional
    @Modifying
    public Map<String, Integer> restore(Long id, boolean clear) {
        RestorePoint rp = restorePointRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("RestorePoint","id",id));
        if (clear) {
            clearCurrentData(rp);
//            appService.delete(rp.getAppId());
        }
        String hash = rp.getHash();
        Map<String, Integer> summary = new HashMap<>();
        if (rp.isIncludeApp()) {
            Arrays.stream(this.TABLE_LIST_APP).forEach(tableName -> {
                try{
                    String[] columns = COLUMN_NAME_MAP.get(tableName).split(",");
                    String setClause = Arrays.stream(columns).map(s -> ACTIVE_DB + "." + tableName + "." + s.trim() + "=" + BACKUP_DB + "." + tableName + "." + s.trim())
                            .collect(Collectors.joining(","));

                    summary.put(tableName, this.entityManager.createNativeQuery("insert into " + ACTIVE_DB + "." + tableName + " (" + COLUMN_NAME_MAP.get(tableName) + ") " +
                                    "select " + COLUMN_NAME_MAP.get(tableName) + " from " + BACKUP_DB + "." + tableName + " where hash = :hash " +
                                    " ON DUPLICATE KEY UPDATE " + setClause)
                            .setParameter("hash", hash)
                            .executeUpdate());
                } catch (Exception e) {
                    System.out.println("TableName:"+tableName+", Error:" + e.getMessage());
                    throw new ExecutionException("TableName:"+tableName+", Error:" + e.getMessage());
                }
            });
        }
//        if (rp.isIncludeUsers()) {
//            Arrays.stream(this.TABLE_LIST_USERS).forEach(tableName -> {
//                try{
//                    String[] columns = COLUMN_NAME_MAP.get(tableName).split(",");
//                    String setClause = Arrays.stream(columns).map(s -> ACTIVE_DB + "." + tableName + "." + s.trim() + "=" + BACKUP_DB + "." + tableName + "." + s.trim())
//                            .collect(Collectors.joining(","));
//
//                    summary.put(tableName, this.entityManager.createNativeQuery("insert into " + ACTIVE_DB + "." + tableName + " (" + COLUMN_NAME_MAP.get(tableName) + ") " +
//                                    "select " + COLUMN_NAME_MAP.get(tableName) + " from " + BACKUP_DB + "." + tableName + " where hash = :hash " +
//                                    " ON DUPLICATE KEY UPDATE " + setClause)
//                            .setParameter("hash", hash)
//                            .executeUpdate());
//                } catch (Exception e) {
//                    System.out.println("TableName:"+tableName+", Error:" + e.getMessage());
//                    throw new ExecutionException("TableName:"+tableName+", Error:" + e.getMessage());
//                }
//            });
//        }
//        if (rp.isIncludeEntry()) {
//            Arrays.stream(this.TABLE_LIST_ENTRY).forEach(tableName -> {
//                try{
//                    String[] columns = COLUMN_NAME_MAP.get(tableName).split(",");
//                    String setClause = Arrays.stream(columns).map(s -> ACTIVE_DB + "." + tableName + "." + s.trim() + "=" + BACKUP_DB + "." + tableName + "." + s.trim())
//                            .collect(Collectors.joining(","));
//
//                    summary.put(tableName, this.entityManager.createNativeQuery("insert into " + ACTIVE_DB + "." + tableName + " (" + COLUMN_NAME_MAP.get(tableName) + ") " +
//                                    "select " + COLUMN_NAME_MAP.get(tableName) + " from " + BACKUP_DB + "." + tableName + " where hash = :hash " +
//                                    " ON DUPLICATE KEY UPDATE " + setClause)
//                            .setParameter("hash", hash)
//                            .executeUpdate());
//                } catch (Exception e) {
//                    System.out.println("TableName:"+tableName+", Error:" + e.getMessage());
//                    throw new ExecutionException("TableName:"+tableName+", Error:" + e.getMessage());
//                }
//            });
//        }

        return summary;
    }


    @Transactional
    public void clearCurrentData(RestorePoint restorePoint) {

        // ### REMOVAL DONE IN REVERSE TO PREVENT CONSTRAINT ERROR

        Map<String, Integer> summary = new HashMap<>();
        if (restorePoint.isIncludeEntry()) {
            appService.deleteEntry(restorePoint.getAppId());
//            List<String> tableEntry = Arrays.asList(this.TABLE_LIST_ENTRY);
//            Collections.reverse(tableEntry);
//            tableEntry.forEach(tableName -> {
//                System.out.println("delete:"+tableName+":::"+DELETE_SQL_MAP.get(tableName).replaceAll("#ACTIVE_DB#", ACTIVE_DB));
//                summary.put(tableName, this.entityManager.createNativeQuery(DELETE_SQL_MAP.get(tableName)
//                        .replaceAll("#ACTIVE_DB#", ACTIVE_DB)
//                )
//                        .setParameter("appId", restorePoint.getAppId())
//                        .executeUpdate());
//            });
        }
//        if (restorePoint.isIncludeUsers()) {
//            appService.deleteUsers(restorePoint.getAppId());
////            List<String> tableUsers = Arrays.asList(this.TABLE_LIST_USERS);
////            Collections.reverse(tableUsers);
////            tableUsers.forEach(tableName -> {
////                System.out.println("delete:"+tableName+":::"+DELETE_SQL_MAP.get(tableName).replaceAll("#ACTIVE_DB#", ACTIVE_DB));
////                summary.put(tableName, this.entityManager.createNativeQuery(DELETE_SQL_MAP.get(tableName)
////                        .replaceAll("#ACTIVE_DB#", ACTIVE_DB)
////                )
////                        .setParameter("appId", restorePoint.getAppId())
////                        .executeUpdate());
////            });
//        }
//        if (restorePoint.isIncludeApp()) {
//            appService.deleteApp(restorePoint.getAppId());
////            List<String> tableApp = Arrays.asList(this.TABLE_LIST_APP);
////            Collections.reverse(tableApp);
////            tableApp.forEach(tableName -> {
////                System.out.println("delete:"+tableName+":::"+DELETE_SQL_MAP.get(tableName).replaceAll("#ACTIVE_DB#", ACTIVE_DB));
////                summary.put(tableName, this.entityManager.createNativeQuery(DELETE_SQL_MAP.get(tableName)
////                        .replaceAll("#ACTIVE_DB#", ACTIVE_DB)
////                )
////                        .setParameter("appId", restorePoint.getAppId())
////                        .executeUpdate());
////            });
//        }
    }


    @Transactional
    public Map<String, Integer> delete(Long id) {

        RestorePoint rp = restorePointRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("RestorePoint","id",id));

        final String hash = rp.getHash();
        Map<String, Integer> summary = new HashMap<>();
        if (rp.isIncludeApp()) {

            Arrays.stream(this.TABLE_LIST_APP).forEach(tableName -> {
                summary.put(tableName, this.entityManager.createNativeQuery("delete from " + BACKUP_DB + "." + tableName + " where hash = :hash")
                        .setParameter("hash", hash)
                        .executeUpdate());
            });
        }
//        if (rp.isIncludeUsers()) {
//            Arrays.stream(this.TABLE_LIST_USERS).forEach(tableName -> {
//                summary.put(tableName, this.entityManager.createNativeQuery("delete from " + BACKUP_DB + "." + tableName + " where hash = :hash")
//                        .setParameter("hash", hash)
//                        .executeUpdate());
//            });
//        }
//        if (rp.isIncludeEntry()) {
//            Arrays.stream(this.TABLE_LIST_ENTRY).forEach(tableName -> {
//                summary.put(tableName, this.entityManager.createNativeQuery("delete from " + BACKUP_DB + "." + tableName + " where hash = :hash")
//                        .setParameter("hash", hash)
//                        .executeUpdate());
//            });
//        }

        restorePointRepository.delete(rp);

        return summary;
    }
}

