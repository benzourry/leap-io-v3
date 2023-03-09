//package com.benzourry.leap.utility.audit;
//
//import org.eclipse.persistence.config.DescriptorCustomizer;
//import org.eclipse.persistence.descriptors.ClassDescriptor;
//
///**
// * Created by RazifBaital on 3/7/2015.
// * For Hibernate, use @SQLDelete(sql = "UPDATE TABLE SET ACTIVE_FLAG = false WHERE id = ?")
// */
//
//public class AuditCustomizer implements DescriptorCustomizer {
//    @Override
//    public void customize(ClassDescriptor classDescriptor) throws Exception {
//        classDescriptor.getQueryManager().setDeleteSQLString("UPDATE "+classDescriptor.getTableName()+" SET ACTIVE_FLAG = '0' WHERE ID = #ID");
//    }
//
//}
