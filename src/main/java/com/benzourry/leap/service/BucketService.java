package com.benzourry.leap.service;

import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.BucketRepository;
import com.benzourry.leap.repository.EntryAttachmentRepository;
import com.benzourry.leap.config.Constant;
import com.benzourry.leap.repository.ItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.benzourry.leap.config.Constant.IO_BASE_DOMAIN;

@Service
public class BucketService {
    private final AppRepository appRepository;
    private final BucketRepository bucketRepository;
    private final EntryAttachmentRepository entryAttachmentRepository;
    private final ItemRepository itemRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public BucketService(AppRepository appRepository,
                         BucketRepository bucketRepository,
                         ItemRepository itemRepository,
                         EntryAttachmentRepository entryAttachmentRepository) {
        this.appRepository = appRepository;
        this.bucketRepository = bucketRepository;
        this.itemRepository = itemRepository;
        this.entryAttachmentRepository = entryAttachmentRepository;
    }

    public Bucket save(Bucket bucket, Long appId) {
//        App app = appRepository.getOne(appId);
        Bucket b;
        // why using hash? is this still in used? from file uploads code, dir using 'bucket-??' for name
        if (bucket.getId() == null) {
            String hash = DigestUtils.md5Hex(Instant.now().getEpochSecond() + ":" + appId).toUpperCase();
            bucket.setCode(hash);
            bucket.setAppId(appId);
            b = bucketRepository.save(bucket);
            File dir = new File(Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + b.getId() + "/");
            dir.mkdirs();
        } else {
            b = bucketRepository.save(bucket);
        }

        return b;
    }

    public Page<Bucket> findByAppId(Long appId, Pageable pageable) {
        return this.bucketRepository.findByAppId(appId, pageable);
    }

    public Bucket findById(Long id) {
        return bucketRepository.getReferenceById(id);
    }

    public void delete(Long id) {
//        entryAttachmentRepository.deleteByBucket(id);
        bucketRepository.deleteById(id);
    }

    public Page<EntryAttachment> findFilesByBucketId(Long bucketId, String searchText, Pageable pageable) {
        searchText = "%" + searchText.toLowerCase() + "%";
        return entryAttachmentRepository.findByBucketId(bucketId, searchText, pageable);
    }

    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> getStat(Long bucketId) {
        Map<String, Object> stat = Map.of("typeCount", entryAttachmentRepository.statCountByFileType(bucketId),
                "typeSize", entryAttachmentRepository.statSizeByFileType(bucketId),
                "labelCount", entryAttachmentRepository.statCountByItemLabel(bucketId),
                "labelSize", entryAttachmentRepository.statSizeByItemLabel(bucketId),
                "totalSize", entryAttachmentRepository.statTotalSize(bucketId),
                "totalCount", entryAttachmentRepository.statTotalCount(bucketId));

        return CompletableFuture.completedFuture(stat);
        // count by type
        // size by type
        // total size
        // count by field
        // size by field

    }

    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> getZipBucket(Long bucketId) throws IOException {
        Map<String, Object> model = new HashMap<>();

        File dir = new File(Constant.UPLOAD_ROOT_DIR + "/tmp/");
        dir.mkdirs();

        String filename = "bucket-" + bucketId + "-" + System.currentTimeMillis() + ".zip";
        String tmpDest = Constant.UPLOAD_ROOT_DIR + "/tmp/" + filename;

        ZipFile zf = new ZipFile(tmpDest);

        long totalWork = zf.getProgressMonitor().getTotalWork();

        zf.addFolder(new File(Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + bucketId));

        model.put("total", totalWork);
        model.put("fileName", filename);
        model.put("filePath", tmpDest);
        model.put("timestamp", Instant.now().toEpochMilli());
        model.put("fileUrl", IO_BASE_DOMAIN + "/bucket/zip-download/" + filename);


        return CompletableFuture.completedFuture(model);
    }

    public Map<String, Object> deleteFile(Long id) {

        Map<String, Object> data = new HashMap<>();
        EntryAttachment entryAttachment = entryAttachmentRepository.getReferenceById(id);

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        if (entryAttachment.getBucketId() != null) {
            destStr += "bucket-" + entryAttachment.getBucketId() + "/";
        }


        File dir = new File(destStr);
        dir.mkdirs();

        File dest = new File(destStr + entryAttachment.getFileUrl());
        data.put("success", dest.delete());


        entryAttachmentRepository.delete(entryAttachment);

        return data;
    }


    public Map<String, Object> deleteFileByEntryId(Long entryId) {

        Map<String, Object> data = new HashMap<>();
        List<EntryAttachment> entryAttachmentList = entryAttachmentRepository.findByEntryId(entryId);

        Map<Long, Object> deletedAttachment = new HashMap<>();

        entryAttachmentList.forEach(entryAttachment -> {
            String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

            if (entryAttachment.getBucketId() != null) {
                destStr += "bucket-" + entryAttachment.getBucketId() + "/";
            }


            File dir = new File(destStr);
            dir.mkdirs();

            File dest = new File(destStr + entryAttachment.getFileUrl());
            deletedAttachment.put(entryAttachment.getId(), dest.delete());


            entryAttachmentRepository.delete(entryAttachment);
        });

        data.put("deleted", deletedAttachment);
        data.put("success", true);
        return data;
    }

    public List<String> filesFromBucket(Long bucketId) {
        return this.bucketRepository.findPathByBucketId(bucketId);
    }

    @Async("asyncExec")
    @Transactional
    public CompletableFuture<Map<String, Object>> reorganize(Long bucketId) {

        Bucket bucket = bucketRepository.findById(bucketId).orElseThrow(); // terminate execution if error

        List<Item> itemList = itemRepository.findByBucketId(bucketId);

        // make sure to create bucket folder if not exist.
        File dir = new File(Constant.UPLOAD_ROOT_DIR + "/attachment/" + "bucket-" + bucketId + "/");
        dir.mkdirs();

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        itemList.forEach(item -> {
            try (Stream<EntryAttachment> eaList = entryAttachmentRepository.findByItemId(item.getId())) {
//                System.out.println("EA by items: "+ eaList.count());
                eaList.forEach(ea -> {
                    String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
                    String oriPath = destStr + ea.getFileUrl();

                    if (ea.getBucketId() != null) { // mn da bucket, fullpath mesti include bucket
                        oriPath = destStr + "bucket-" + ea.getBucketId() + "/" + ea.getFileUrl();
                    }

                    String newPath = destStr + "bucket-" + bucket.getId() + "/" + ea.getFileUrl();

                    try {
                        Files.move(
                                Paths.get(oriPath),
                                Paths.get(newPath),
                                StandardCopyOption.REPLACE_EXISTING
                        );

                        ea.setBucketId(bucket.getId());

                        entryAttachmentRepository.saveAndFlush(ea);

                        success.getAndIncrement();

                    } catch (IOException ioe) {
                        failed.getAndIncrement();
                        System.out.println("Error transfering bucket [EA-ID:" + ea.getId() + "] >> " + ioe.getMessage());
                    }
                    this.entityManager.detach(ea);
                });
            }
        });

        return CompletableFuture.completedFuture(Map.of("success", success.get(), "failure", failed.get()));

    }

//    public List<Bucket> findRegListByAppId(Long appId) {
//        return this.bucketRepository.findRegListByAppId(appId);
//    }
}
