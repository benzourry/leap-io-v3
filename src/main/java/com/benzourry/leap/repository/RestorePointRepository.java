package com.benzourry.leap.repository;


import com.benzourry.leap.model.RestorePoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RestorePointRepository extends JpaRepository<RestorePoint, Long> {

    @Query(value = "select * from restore_point f where f.app_id = :appId", nativeQuery = true)
    Page<RestorePoint> findByAppId(@Param("appId") Long appId, Pageable pageable);

//    List<PushSub> findPushSubsByUser_Id(Long userId);
//    List<PushSub> findPushSubsByUser_IdAndActiveIsTrue(Long userId);

//    boolean existsByUser_IdAndUserAgent(Long userId, String userAgent);

//    @Query("select count(e)>0 from PushSub e where e.endpoint = :endpoint OR (e.user.id = :userId and e.userAgent = :userAgent)")
//    boolean existsCheck(@Param("endpoint") String endpoint,
//                                         @Param("userId") Long userId,
//                                         @Param("userAgent") String userAgent);
//
//    @Query("select e from PushSub e where e.endpoint = :endpoint OR (e.user.id = :userId and e.userAgent = :userAgent)")
//    PushSub findCheck(@Param("endpoint") String endpoint,
//                                         @Param("userId") Long userId,
//                                         @Param("userAgent") String userAgent);

//    List<PushSub> findPushSubsByAppId(Long appId);
    /* ###BACKUP*/
//
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.app (`id`, `app_path`, `clone`, `description`, `email`, `layout`, `logo`, `navi`, `price`, `public_access`, `secret`, `shared`, `status`, `tag`, `theme`, `title`, `use_email`, `use_facebook`, `use_github`, `use_google`, `use_linkedin`, `use_unimas`, `app_domain`, `block_anon`, `start_page`, `reg`, `use_anon`, `f`, `x`, `once`, `can_push`, `hash`) " +
//            "select `id`, `app_path`, `clone`, `description`, `email`, `layout`, `logo`, `navi`, `price`, `public_access`, `secret`, `shared`, `status`, `tag`, `theme`, `title`, `use_email`, `use_facebook`, `use_github`, `use_google`, `use_linkedin`, `use_unimas`, `app_domain`, `block_anon`, `start_page`, `reg`, `use_anon`, `f`, `x`, `once`, `can_push`,:hash from leap_io.app " +
//            "where id = :appId",nativeQuery = true)
//    int backupApp(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.lookup (`id`, `code_prop`, `data_enabled`, `desc_prop`, `description`, `email`, `endpoint`, `extra_prop`, `headers`, `json_root`, `name`, `response_type`, `shared`, `source_type`, `proxy_id`, `access`, `app`, `data_fields`, `auth`, `client_id`, `client_secret`, `token_endpoint`, `hash`)" +
//            "select `id`, `code_prop`, `data_enabled`, `desc_prop`, `description`, `email`, `endpoint`, `extra_prop`, `headers`, `json_root`, `name`, `response_type`, `shared`, `source_type`, `proxy_id`, `access`, `app`, `data_fields`, `auth`, `client_id`, `client_secret`, `token_endpoint`, :hash from leap_io.lookup " +
//            "where app = :appId",nativeQuery = true)
//    int backupLookup(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.lookup_entry (`id`, `code`, `data`, `enabled`, `extra`, `name`, `ordering`, `lookup`, `hash`)" +
//            "select `id`, `code`, `data`, `enabled`, `extra`, `name`, `ordering`, `lookup`, :hash from leap_io.lookup_entry " +
//            "where lookup in (select id from leap_io.lookup where app = :appId)",nativeQuery = true)
//    int backupLookupEntry(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.user_group (`id`, `name`, `users`, `app`, `allow_reg`, `description`, `need_approval`, `hash`) " +
//            "select `id`, `name`, `users`, `app`, `allow_reg`, `description`, `need_approval`, :hash from leap_io.user_group " +
//            "where app = :appId",nativeQuery = true)
//    int backupUserGroup(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.email_template (`id`, `cc_admin`, `cc_approver`, `cc_user`, `content`, `creator`, `description`, `enabled`, `name`, `shared`, `subject`, `to_admin`, `to_approver`, `to_user`, `app`, `pickable`, `pushable`, `push_url`, `hash`) " +
//            "select `id`, `cc_admin`, `cc_approver`, `cc_user`, `content`, `creator`, `description`, `enabled`, `name`, `shared`, `subject`, `to_admin`, `to_approver`, `to_user`, `app`, `pickable`, `pushable`, `push_url`,:hash from leap_io.email_template " +
//            "where app = :appId",nativeQuery = true)
//    int backupEmailTemplate(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.endpoint (`id`, `auth`, `client_id`, `client_secret`, `code`, `description`, `email`, `headers`, `json_root`, `method`, `name`, `response_type`, `shared`, `token_endpoint`, `url`, `app`, `hash`) " +
//            "select `id`, `auth`, `client_id`, `client_secret`, `code`, `description`, `email`, `headers`, `json_root`, `method`, `name`, `response_type`, `shared`, `token_endpoint`, `url`, `app`, :hash from leap_io.endpoint " +
//            "where app = :appId",nativeQuery = true)
//    int backupEndpoint(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.form (`id`, `admin`, `align`, `code_format`, `description`, `end_date`, `f`, `icon`, `inactive`, `nav`, `public_access`, `start_date`, `title`, `access`, `app`, `prev`, `counter`, `can_edit`, `can_retract`, `can_save`, `can_submit`, `validate_save`, `add_mailer`, `hide_status`, `on_save`, `on_submit`, `on_view`, `retract_mailer`, `single`, `single_q`, `update_appr_mailer`, `update_mailer`, `show_index`, `x`, `public_ep`, `hash`) " +
//            "select `id`, `admin`, `align`, `code_format`, `description`, `end_date`, `f`, `icon`, `inactive`, `nav`, `public_access`, `start_date`, `title`, `access`, `app`, `prev`, `counter`, `can_edit`, `can_retract`, `can_save`, `can_submit`, `validate_save`, `add_mailer`, `hide_status`, `on_save`, `on_submit`, `on_view`, `retract_mailer`, `single`, `single_q`, `update_appr_mailer`, `update_mailer`, `show_index`, `x`, `public_ep`, :hash from leap_io.form " +
//            "where app = :appId",nativeQuery = true)
//    int backupForm(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.tab (`id`, `code`, `pre`, `sort_order`, `title`, `form`, `hash`) " +
//            "select `id`, `code`, `pre`, `sort_order`, `title`, `form`, :hash from leap_io.tab " +
//            "where form in (select id from leap_io.form " +
//            "where app = :appId)",nativeQuery = true)
//    int backupFormTab(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.item (`id`, `bind_label`, `code`, `datasource`, `data_source_init`, `f`, `hidden`, `hide_label`, `hint`, `label`, `options`, `placeholder`, `post`, `pre`, `read_only`, `size`, `sub_type`, `type`, `v`, `form`, `format`, `hash`)" +
//            "select `id`, `bind_label`, `code`, `datasource`, `data_source_init`, `f`, `hidden`, `hide_label`, `hint`, `label`, `options`, `placeholder`, `post`, `pre`, `read_only`, `size`, `sub_type`, `type`, `v`, `form`, `format`, :hash from leap_io.item " +
//            "where form in (select id from leap_io.form " +
//            "where app = :appId)",nativeQuery = true)
//    int backupFormItem(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.section (`id`, `align`, `code`, `description`, `enabled_for`, `hide_header`, `main`, `parent`, `pre`, `size`, `sort_order`, `title`, `type`, `form`, `inline`, `style`, `max_child`, `icon`, `add_label`, `hash`) " +
//            "select `id`, `align`, `code`, `description`, `enabled_for`, `hide_header`, `main`, `parent`, `pre`, `size`, `sort_order`, `title`, `type`, `form`, `inline`, `style`, `max_child`, `icon`, `add_label`, :hash from leap_io.section " +
//            "where form in (select id from leap_io.form " +
//            "where app = :appId)",nativeQuery = true)
//    int backupFormSection(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.section_item (`id`, `code`, `sort_order`, `section`, `hash`) " +
//            "select `id`, `code`, `sort_order`, `section`, :hash from leap_io.section_item " +
//            "where section in (select id from leap_io.section " +
//            "where form in (select id from leap_io.form " +
//            "where app = :appId))",nativeQuery = true)
//    int backupFormSectionItem(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.tier (`id`, `always_approve`, `approve_mailer`, `approver`, `assign_mailer`, `can_approve`, `can_reject`, `can_remark`, `can_return`, `can_skip`, `name`, `org_map`, `org_map_pointer`, `reject_mailer`, `resubmit_mailer`, `return_mailer`, `show_approver`, `sort_order`, `submit_mailer`, `type`, `form`, `form_section`, `org_map_param`, `post`, `pre`, `assigner`, `hash`) " +
//            "select `id`, `always_approve`, `approve_mailer`, `approver`, `assign_mailer`, `can_approve`, `can_reject`, `can_remark`, `can_return`, `can_skip`, `name`, `org_map`, `org_map_pointer`, `reject_mailer`, `resubmit_mailer`, `return_mailer`, `show_approver`, `sort_order`, `submit_mailer`, `type`, `form`, `form_section`, `org_map_param`, `post`, `pre`, `assigner`, :hash from leap_io.tier " +
//            "where form in (select id from leap_io.form " +
//            "where app = :appId)",nativeQuery = true)
//    int backupTier(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.tier_action (`id`, `action`, `code`, `color`, `icon`, `label`, `mailer`, `next_tier`, `pre`, `sort_order`, `user_edit`, `tier`, `hash`) " +
//            "select `id`, `action`, `code`, `color`, `icon`, `label`, `mailer`, `next_tier`, `pre`, `sort_order`, `user_edit`, `tier`, :hash from leap_io.tier_action " +
//            "where tier in (select id from leap_io.tier " +
//            "where form in (select id from leap_io.form " +
//            "where app = :appId))",nativeQuery = true)
//    int backupTierAction(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.dashboard (`id`, `code`, `description`, `size`, `sort_order`, `title`, `type`, `access`, `app`, `wide`, `hash`) " +
//            "select `id`, `code`, `description`, `size`, `sort_order`, `title`, `type`, `access`, `app`, `wide`, :hash from leap_io.dashboard " +
//            "where app = :appId",nativeQuery = true)
//    int backupDashboard(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.chart (`id`, `agg`, `can_view`, `description`, `field_code`, `field_value`, `height`, `root_code`, `root_value`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `dashboard`, `form`, `preset_filters`, `field_series`, `root_series`, `series`, `show_agg`, `x`, `endpoint`, `f`, `source_type`, `hash`) " +
//            "select `id`, `agg`, `can_view`, `description`, `field_code`, `field_value`, `height`, `root_code`, `root_value`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `dashboard`, `form`, `preset_filters`, `field_series`, `root_series`, `series`, `show_agg`, `x`, `endpoint`, `f`, `source_type`, :hash from leap_io.chart " +
//            "where dashboard in (select id from leap_io.dashboard " +
//            "where app = :appId)",nativeQuery = true)
//    int backupChart(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.chart_filter (`id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `chart`, `hash`) " +
//            "select `id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `chart`, :hash from leap_io.chart_filter " +
//            "where chart in (select id from leap_io.chart " +
//            "where dashboard in (select id from leap_io.dashboard " +
//            "where app = :appId))",nativeQuery = true)
//    int backupChartFilter(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.dataset (`id`, `admin_only`, `can_delete`, `can_edit`, `can_retract`, `can_view`, `code`, `description`, `export_csv`, `export_pdf`, `export_pdf_layout`, `export_xls`, `next`, `preset_filters`, `show_action`, `show_status`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `ui`, `ui_template`, `access`, `app`, `form`, `screen`, `wide`, `blast_to`, `can_blast`, `can_reset`, `default_sort`, `show_index`, `public_ep`, `hash`) " +
//            "select `id`, `admin_only`, `can_delete`, `can_edit`, `can_retract`, `can_view`, `code`, `description`, `export_csv`, `export_pdf`, `export_pdf_layout`, `export_xls`, `next`, `preset_filters`, `show_action`, `show_status`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `ui`, `ui_template`, `access`, `app`, `form`, `screen`, `wide`, `blast_to`, `can_blast`, `can_reset`, `default_sort`, `show_index`, `public_ep`, :hash from leap_io.dataset " +
//            "where app = :appId",nativeQuery = true)
//    int backupDataset(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.dataset_item (`id`, `code`, `form_id`, `label`, `root`, `sort_order`, `dataset`, `type`, `prefix`, `hash`) " +
//            "select `id`, `code`, `form_id`, `label`, `root`, `sort_order`, `dataset`, `type`, `prefix`, :hash from leap_io.dataset_item " +
//            "where dataset in (select id from leap_io.dataset " +
//            "where app = :appId)",nativeQuery = true)
//    int backupDatasetItem(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.dataset_filter (`id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `dataset`, `prefix`, `type`, `hash`) " +
//            "select `id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `dataset`, `prefix`, `type`, :hash from leap_io.dataset_filter " +
//            "where dataset in (select id from leap_io.dataset " +
//            "where app = :appId)",nativeQuery = true)
//    int backupDatasetFilter(@Param("appId") Long appId, @Param("hash") String hash);
//
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.screen (`id`, `data`, `description`, `next`, `sort_order`, `title`, `type`, `access`, `app`, `dataset`, `form`, `can_print`, `wide`, `show_action`, `hash`) " +
//            "select `id`, `data`, `description`, `next`, `sort_order`, `title`, `type`, `access`, `app`, `dataset`, `form`, `can_print`, `wide`, `show_action`, :hash from leap_io.screen " +
//            "where app = :appId",nativeQuery = true)
//    int backupScreen(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.action (`id`, `label`, `next`, `next_type`, `type`, `screen`, `params`, `hash`) " +
//            "select `id`, `label`, `next`, `next_type`, `type`, `screen`, `params`, :hash from leap_io.action " +
//            "where screen in (select id from leap_io.screen " +
//            "where app = :appId)",nativeQuery = true)
//    int backupScreenAction(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.navi_group (`id`, `sort_order`, `title`, `access`, `app`, `hash`) " +
//            "select `id`, `sort_order`, `title`, `access`, `app`, :hash from leap_io.navi_group " +
//            "where app = :appId",nativeQuery = true)
//    int backupNavi(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.navi_item (`id`, `fl`, `screen_id`, `sort_order`, `title`, `type`, `url`, `navi_group`, `icon`, `pre`, `hash`) " +
//            "select `id`, `fl`, `screen_id`, `sort_order`, `title`, `type`, `url`, `navi_group`, `icon`, `pre`, :hash from leap_io.navi_item " +
//            "where navi_group in (select id from leap_io.navi_group " +
//            "where app = :appId)",nativeQuery = true)
//    int backupNaviItem(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.schedule (`id`, `clock`, `dataset_id`, `day_of_month`, `day_of_week`, `description`, `email`, `enabled`, `freq`, `mailer_id`, `name`, `type`, `app`, `month_of_year`, `hash`) " +
//            "select `id`, `clock`, `dataset_id`, `day_of_month`, `day_of_week`, `description`, `email`, `enabled`, `freq`, `mailer_id`, `name`, `type`, `app`, `month_of_year`, :hash from leap_io.schedule " +
//            "where app = :appId",nativeQuery = true)
//    int backupSchedule(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.users (`id`, `email`, `email_verified`, `image_url`, `name`, `password`, `provider`, `provider_id`, `app_id`, `attributes`, `first_login`, `last_login`, `status`, `once`, `hash`) " +
//            "select `id`, `email`, `email_verified`, `image_url`, `name`, `password`, `provider`, `provider_id`, `app_id`, `attributes`, `first_login`, `last_login`, `status`, `once`, :hash from leap_io.users " +
//            "where app_id = :appId",nativeQuery = true)
//    int backupUsers(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.app_user (`id`, `status`, `user_group`, `user`, `hash`) " +
//            "select `id`, `status`, `user_group`, `user`, :hash from leap_io.app_user " +
//            "where user in (select id from leap_io.users " +
//            "where app_id = :appId)",nativeQuery = true)
//    int backupAppUsers(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.entry (`id`, `current_status`, `current_tier`, `current_tier_id`, `data`, `email`, `final_tier_id`, `prev`, `resubmission_date`, `submission_date`, `form`, `created_by`, `created_date`, `modified_by`, `modified_date`, `current_edit`, `hash`) " +
//            "select `id`, `current_status`, `current_tier`, `current_tier_id`, `data`, `email`, `final_tier_id`, `prev`, `resubmission_date`, `submission_date`, `form`, `created_by`, `created_date`, `modified_by`, `modified_date`, `current_edit`, :hash from leap_io.entry " +
//            "where form in (select id from leap_io.form " +
//            "where app = :appId)",nativeQuery = true)
//    int backupEntry(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.entry_approval (`id`, `data`, `email`, `remark`, `status`, `timestamp`, `entry`, `tier`, `tier_id`, `hash`) " +
//            "select `id`, `data`, `email`, `remark`, `status`, `timestamp`, `entry`, `tier`, `tier_id`, :hash from leap_io.entry_approval " +
//            "where entry in (select id from leap_io.entry " +
//            "where form in (select id from leap_io.form " +
//            "where app = :appId))",nativeQuery = true)
//    int backupEntryApproval(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.entry_approval_trail (`id`, `data`, `email`, `entry_id`, `remark`, `status`, `timestamp`, `tier`, `hash`) " +
//            "select `id`, `data`, `email`, `entry_id`, `remark`, `status`, `timestamp`, `tier`, :hash from leap_io.entry_approval_trail " +
//            "where entry_id in (select id from leap_io.entry " +
//            "where form in (select id from leap_io.form " +
//            "where app = :appId))",nativeQuery = true)
//    int backupEntryApprovalTrail(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.entry_approver (`entry_id`, `approver`, `tier_id`, `hash`) " +
//            "select `entry_id`, `approver`, `tier_id`, :hash from leap_io.entry_approver " +
//            "where entry_id in (select id from leap_io.entry " +
//            "where form in (select id from leap_io.form " +
//            "where app = :appId))",nativeQuery = true)
//    int backupEntryApprover(@Param("appId") Long appId, @Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "insert ignore into backup_leap_io.entry_attachment (`id`, `file_name`, `file_size`, `file_type`, `file_url`, `item_id`, `message`, `success`, `timestamp`, `hash`) " +
//            "select `id`, `file_name`, `file_size`, `file_type`, `file_url`, `item_id`, `message`, `success`, `timestamp`, :hash from leap_io.entry_attachment " +
//            "where item_id in (select id from leap_io.item " +
//            "where form in (select id from leap_io.form " +
//            "where app = :appId))",nativeQuery = true)
//    int backupEntryAttachment(@Param("appId") Long appId, @Param("hash") String hash);
//
//    /* ###RESTORE*/
//
//
//    @Modifying
//    @Query(value = "replace into leap_io.app (`id`, `app_path`, `clone`, `description`, `email`, `layout`, `logo`, `navi`, `price`, `public_access`, `secret`, `shared`, `status`, `tag`, `theme`, `title`, `use_email`, `use_facebook`, `use_github`, `use_google`, `use_linkedin`, `use_unimas`, `app_domain`, `block_anon`, `start_page`, `reg`, `use_anon`, `f`, `x`, `once`, `can_push`) " +
//            "select `id`, `app_path`, `clone`, `description`, `email`, `layout`, `logo`, `navi`, `price`, `public_access`, `secret`, `shared`, `status`, `tag`, `theme`, `title`, `use_email`, `use_facebook`, `use_github`, `use_google`, `use_linkedin`, `use_unimas`, `app_domain`, `block_anon`, `start_page`, `reg`, `use_anon`, `f`, `x`, `once`, `can_push` from backup_leap_io.app " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreApp(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.lookup (`id`, `code_prop`, `data_enabled`, `desc_prop`, `description`, `email`, `endpoint`, `extra_prop`, `headers`, `json_root`, `name`, `response_type`, `shared`, `source_type`, `proxy_id`, `access`, `app`, `data_fields`, `auth`, `client_id`, `client_secret`, `token_endpoint`)" +
//            "select `id`, `code_prop`, `data_enabled`, `desc_prop`, `description`, `email`, `endpoint`, `extra_prop`, `headers`, `json_root`, `name`, `response_type`, `shared`, `source_type`, `proxy_id`, `access`, `app`, `data_fields`, `auth`, `client_id`, `client_secret`, `token_endpoint` from backup_leap_io.lookup " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreLookup(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.lookup_entry (`id`, `code`, `data`, `enabled`, `extra`, `name`, `ordering`, `lookup` )" +
//            "select `id`, `code`, `data`, `enabled`, `extra`, `name`, `ordering`, `lookup` from backup_leap_io.lookup_entry " +
//            "where hash=:hash",nativeQuery = true)
//    int restoreLookupEntry(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.user_group (`id`, `name`, `users`, `app`, `allow_reg`, `description`, `need_approval` ) " +
//            "select `id`, `name`, `users`, `app`, `allow_reg`, `description`, `need_approval` from backup_leap_io.user_group " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreUserGroup(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.email_template (`id`, `cc_admin`, `cc_approver`, `cc_user`, `content`, `creator`, `description`, `enabled`, `name`, `shared`, `subject`, `to_admin`, `to_approver`, `to_user`, `app`, `pickable`, `pushable`, `push_url` ) " +
//            "select `id`, `cc_admin`, `cc_approver`, `cc_user`, `content`, `creator`, `description`, `enabled`, `name`, `shared`, `subject`, `to_admin`, `to_approver`, `to_user`, `app`, `pickable`, `pushable`, `push_url` from backup_leap_io.email_template " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreEmailTemplate(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.endpoint (`id`, `auth`, `client_id`, `client_secret`, `code`, `description`, `email`, `headers`, `json_root`, `method`, `name`, `response_type`, `shared`, `token_endpoint`, `url`, `app` ) " +
//            "select `id`, `auth`, `client_id`, `client_secret`, `code`, `description`, `email`, `headers`, `json_root`, `method`, `name`, `response_type`, `shared`, `token_endpoint`, `url`, `app` from backup_leap_io.endpoint " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreEndpoint(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.form (`id`, `admin`, `align`, `code_format`, `description`, `end_date`, `f`, `icon`, `inactive`, `nav`, `public_access`, `start_date`, `title`, `access`, `app`, `prev`, `counter`, `can_edit`, `can_retract`, `can_save`, `can_submit`, `validate_save`, `add_mailer`, `hide_status`, `on_save`, `on_submit`, `on_view`, `retract_mailer`, `single`, `single_q`, `update_appr_mailer`, `update_mailer`, `show_index`, `x`, `public_ep` ) " +
//            "select `id`, `admin`, `align`, `code_format`, `description`, `end_date`, `f`, `icon`, `inactive`, `nav`, `public_access`, `start_date`, `title`, `access`, `app`, `prev`, `counter`, `can_edit`, `can_retract`, `can_save`, `can_submit`, `validate_save`, `add_mailer`, `hide_status`, `on_save`, `on_submit`, `on_view`, `retract_mailer`, `single`, `single_q`, `update_appr_mailer`, `update_mailer`, `show_index`, `x`, `public_ep` from backup_leap_io.form " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreForm(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.tab (`id`, `code`, `pre`, `sort_order`, `title`, `form` ) " +
//            "select `id`, `code`, `pre`, `sort_order`, `title`, `form` from backup_leap_io.tab " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreFormTab(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.item (`id`, `bind_label`, `code`, `datasource`, `data_source_init`, `f`, `hidden`, `hide_label`, `hint`, `label`, `options`, `placeholder`, `post`, `pre`, `read_only`, `size`, `sub_type`, `type`, `v`, `form`, `format` )" +
//            "select `id`, `bind_label`, `code`, `datasource`, `data_source_init`, `f`, `hidden`, `hide_label`, `hint`, `label`, `options`, `placeholder`, `post`, `pre`, `read_only`, `size`, `sub_type`, `type`, `v`, `form`, `format` from backup_leap_io.item " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreFormItem(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.section (`id`, `align`, `code`, `description`, `enabled_for`, `hide_header`, `main`, `parent`, `pre`, `size`, `sort_order`, `title`, `type`, `form`, `inline`, `style`, `max_child`, `icon`, `add_label` ) " +
//            "select `id`, `align`, `code`, `description`, `enabled_for`, `hide_header`, `main`, `parent`, `pre`, `size`, `sort_order`, `title`, `type`, `form`, `inline`, `style`, `max_child`, `icon`, `add_label` from backup_leap_io.section " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreFormSection(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.section_item (`id`, `code`, `sort_order`, `section` ) " +
//            "select `id`, `code`, `sort_order`, `section` from backup_leap_io.section_item " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreFormSectionItem(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.tier (`id`, `always_approve`, `approve_mailer`, `approver`, `assign_mailer`, `can_approve`, `can_reject`, `can_remark`, `can_return`, `can_skip`, `name`, `org_map`, `org_map_pointer`, `reject_mailer`, `resubmit_mailer`, `return_mailer`, `show_approver`, `sort_order`, `submit_mailer`, `type`, `form`, `form_section`, `org_map_param`, `post`, `pre`, `assigner` ) " +
//            "select `id`, `always_approve`, `approve_mailer`, `approver`, `assign_mailer`, `can_approve`, `can_reject`, `can_remark`, `can_return`, `can_skip`, `name`, `org_map`, `org_map_pointer`, `reject_mailer`, `resubmit_mailer`, `return_mailer`, `show_approver`, `sort_order`, `submit_mailer`, `type`, `form`, `form_section`, `org_map_param`, `post`, `pre`, `assigner` from backup_leap_io.tier " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreTier(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.tier_action (`id`, `action`, `code`, `color`, `icon`, `label`, `mailer`, `next_tier`, `pre`, `sort_order`, `user_edit`, `tier` ) " +
//            "select `id`, `action`, `code`, `color`, `icon`, `label`, `mailer`, `next_tier`, `pre`, `sort_order`, `user_edit`, `tier` from backup_leap_io.tier_action " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreTierAction(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.dashboard (`id`, `code`, `description`, `size`, `sort_order`, `title`, `type`, `access`, `app`, `wide` ) " +
//            "select `id`, `code`, `description`, `size`, `sort_order`, `title`, `type`, `access`, `app`, `wide` from backup_leap_io.dashboard " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreDashboard(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.chart (`id`, `agg`, `can_view`, `description`, `field_code`, `field_value`, `height`, `root_code`, `root_value`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `dashboard`, `form`, `preset_filters`, `field_series`, `root_series`, `series`, `show_agg`, `x`, `endpoint`, `f`, `source_type` ) " +
//            "select `id`, `agg`, `can_view`, `description`, `field_code`, `field_value`, `height`, `root_code`, `root_value`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `dashboard`, `form`, `preset_filters`, `field_series`, `root_series`, `series`, `show_agg`, `x`, `endpoint`, `f`, `source_type` from backup_leap_io.chart " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreChart(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.chart_filter (`id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `chart` ) " +
//            "select `id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `chart` from backup_leap_io.chart_filter " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreChartFilter(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.dataset (`id`, `admin_only`, `can_delete`, `can_edit`, `can_retract`, `can_view`, `code`, `description`, `export_csv`, `export_pdf`, `export_pdf_layout`, `export_xls`, `next`, `preset_filters`, `show_action`, `show_status`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `ui`, `ui_template`, `access`, `app`, `form`, `screen`, `wide`, `blast_to`, `can_blast`, `can_reset`, `default_sort`, `show_index`, `public_ep` ) " +
//            "select `id`, `admin_only`, `can_delete`, `can_edit`, `can_retract`, `can_view`, `code`, `description`, `export_csv`, `export_pdf`, `export_pdf_layout`, `export_xls`, `next`, `preset_filters`, `show_action`, `show_status`, `size`, `sort_order`, `status`, `status_filter`, `title`, `type`, `ui`, `ui_template`, `access`, `app`, `form`, `screen`, `wide`, `blast_to`, `can_blast`, `can_reset`, `default_sort`, `show_index`, `public_ep` from backup_leap_io.dataset " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreDataset(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.dataset_item (`id`, `code`, `form_id`, `label`, `root`, `sort_order`, `dataset`, `type`, `prefix` ) " +
//            "select `id`, `code`, `form_id`, `label`, `root`, `sort_order`, `dataset`, `type`, `prefix` from backup_leap_io.dataset_item " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreDatasetItem(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.dataset_filter (`id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `dataset`, `prefix`, `type` ) " +
//            "select `id`, `code`, `form_id`, `label`, `preset`, `root`, `sort_order`, `dataset`, `prefix`, `type` from backup_leap_io.dataset_filter " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreDatasetFilter(@Param("hash") String hash);
//
//
//    @Modifying
//    @Query(value = "replace into leap_io.screen (`id`, `data`, `description`, `next`, `sort_order`, `title`, `type`, `access`, `app`, `dataset`, `form`, `can_print`, `wide`, `show_action` ) " +
//            "select `id`, `data`, `description`, `next`, `sort_order`, `title`, `type`, `access`, `app`, `dataset`, `form`, `can_print`, `wide`, `show_action` from backup_leap_io.screen " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreScreen(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.action (`id`, `label`, `next`, `next_type`, `type`, `screen`, `params` ) " +
//            "select `id`, `label`, `next`, `next_type`, `type`, `screen`, `params` from backup_leap_io.action " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreScreenAction(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.navi_group (`id`, `sort_order`, `title`, `access`, `app` ) " +
//            "select `id`, `sort_order`, `title`, `access`, `app` from backup_leap_io.navi_group " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreNavi(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.navi_item (`id`, `fl`, `screen_id`, `sort_order`, `title`, `type`, `url`, `navi_group`, `icon`, `pre` ) " +
//            "select `id`, `fl`, `screen_id`, `sort_order`, `title`, `type`, `url`, `navi_group`, `icon`, `pre` from backup_leap_io.navi_item " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreNaviItem(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.schedule (`id`, `clock`, `dataset_id`, `day_of_month`, `day_of_week`, `description`, `email`, `enabled`, `freq`, `mailer_id`, `name`, `type`, `app`, `month_of_year` ) " +
//            "select `id`, `clock`, `dataset_id`, `day_of_month`, `day_of_week`, `description`, `email`, `enabled`, `freq`, `mailer_id`, `name`, `type`, `app`, `month_of_year` from backup_leap_io.schedule " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreSchedule(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.users (`id`, `email`, `email_verified`, `image_url`, `name`, `password`, `provider`, `provider_id`, `app_id`, `attributes`, `first_login`, `last_login`, `status`, `once` ) " +
//            "select `id`, `email`, `email_verified`, `image_url`, `name`, `password`, `provider`, `provider_id`, `app_id`, `attributes`, `first_login`, `last_login`, `status`, `once` from backup_leap_io.users " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreUsers(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.app_user (`id`, `status`, `user_group`, `user` ) " +
//            "select `id`, `status`, `user_group`, `user` from backup_leap_io.app_user " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreAppUsers(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.entry (`id`, `current_status`, `current_tier`, `current_tier_id`, `data`, `email`, `final_tier_id`, `prev`, `resubmission_date`, `submission_date`, `form`, `created_by`, `created_date`, `modified_by`, `modified_date`, `current_edit` ) " +
//            "select `id`, `current_status`, `current_tier`, `current_tier_id`, `data`, `email`, `final_tier_id`, `prev`, `resubmission_date`, `submission_date`, `form`, `created_by`, `created_date`, `modified_by`, `modified_date`, `current_edit` from backup_leap_io.entry " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreEntry(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.entry_approval (`id`, `data`, `email`, `remark`, `status`, `timestamp`, `entry`, `tier`, `tier_id` ) " +
//            "select `id`, `data`, `email`, `remark`, `status`, `timestamp`, `entry`, `tier`, `tier_id` from backup_leap_io.entry_approval " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreEntryApproval(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.entry_approval_trail (`id`, `data`, `email`, `entry_id`, `remark`, `status`, `timestamp`, `tier` ) " +
//            "select `id`, `data`, `email`, `entry_id`, `remark`, `status`, `timestamp`, `tier` from backup_leap_io.entry_approval_trail " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreEntryApprovalTrail(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.entry_approver (`entry_id`, `approver`, `tier_id` ) " +
//            "select `entry_id`, `approver`, `tier_id` from backup_leap_io.entry_approver " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreEntryApprover(@Param("hash") String hash);
//
//    @Modifying
//    @Query(value = "replace into leap_io.entry_attachment (`id`, `file_name`, `file_size`, `file_type`, `file_url`, `item_id`, `message`, `success`, `timestamp` ) " +
//            "select `id`, `file_name`, `file_size`, `file_type`, `file_url`, `item_id`, `message`, `success`, `timestamp` from backup_leap_io.entry_attachment " +
//            "where hash = :hash",nativeQuery = true)
//    int restoreEntryAttachment(@Param("hash") String hash);

}
