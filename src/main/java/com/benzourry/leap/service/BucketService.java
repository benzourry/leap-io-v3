package com.benzourry.leap.service;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.Bucket;
import com.benzourry.leap.model.EntryAttachment;
import com.benzourry.leap.model.Item;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.BucketRepository;
import com.benzourry.leap.repository.EntryAttachmentRepository;
import com.benzourry.leap.repository.ItemRepository;
import com.benzourry.leap.utility.ClamAVServiceUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    final ClamAVServiceUtil clamavService;

    @PersistenceContext
    private EntityManager entityManager;

    public BucketService(AppRepository appRepository,
                         BucketRepository bucketRepository,
                         ItemRepository itemRepository,
                         ClamAVServiceUtil clamavService,
                         EntryAttachmentRepository entryAttachmentRepository) {
        this.appRepository = appRepository;
        this.bucketRepository = bucketRepository;
        this.itemRepository = itemRepository;
        this.clamavService = clamavService;
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


    public EntryAttachment addUrlToBucket(Long bucketId, String url, Long appId, String email) {

        URL website = null;
        try {
            website = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        String filePath = Instant.now().getEpochSecond() + "_" + Long.toString(UUID.randomUUID().getLeastSignificantBits(), Character.MAX_RADIX) +".png";
        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + bucketId + "/" + filePath;


        EntryAttachment attachment = new EntryAttachment();
        File file = new File(destStr);

        try {
            FileUtils.copyURLToFile(website, file);
            attachment.setFileName(filePath);
            attachment.setFileSize(file.length());
            attachment.setFileType("image/png");
            attachment.setFileUrl(filePath);
            attachment.setEmail(email);
            attachment.setTimestamp(new Date());
            attachment.setMessage("success");
            attachment.setItemId(null);
            attachment.setBucketId(bucketId);
            attachment.setAppId(appId);
            entryAttachmentRepository.save(attachment);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return attachment;
    }

    public Page<Bucket> findByAppId(Long appId, Pageable pageable) {
        return this.bucketRepository.findByAppId(appId, pageable);
    }

    public Bucket findById(Long id) {
        return bucketRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Bucket","id",id));
    }

    public void delete(Long id) {
        bucketRepository.deleteById(id);
    }

    public Page<EntryAttachment> findFilesByBucketId(Long bucketId, String searchText, Pageable pageable) {
        searchText = "%" + searchText.toLowerCase() + "%";
        return entryAttachmentRepository.findByBucketId(bucketId, searchText, pageable);
    }

    public Page<EntryAttachment> findFilesByBucketIdAndParams(Long bucketId, String searchText, String email, String fileType, Long entryId, String sStatus, Long itemId,Pageable pageable) {
        searchText = "%" + searchText.toLowerCase() + "%";
//        return entryAttachmentRepository.findByBucketId(bucketId, searchText, pageable);
        return entryAttachmentRepository.findByBucketIdAndParams(bucketId, searchText, email, fileType, entryId, sStatus, itemId, pageable);
    }

    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> getStat(Long bucketId) {
        Map<String, Object> stat = Map.of("typeCount", Optional.ofNullable(entryAttachmentRepository.statCountByFileType(bucketId)).orElse(List.of()),
                "typeSize", Optional.ofNullable(entryAttachmentRepository.statSizeByFileType(bucketId)).orElse(List.of()),
                "labelCount", Optional.ofNullable(entryAttachmentRepository.statCountByItemLabel(bucketId)).orElse(List.of()),
                "labelSize", Optional.ofNullable(entryAttachmentRepository.statSizeByItemLabel(bucketId)).orElse(List.of()),
                "totalSize", Optional.ofNullable(entryAttachmentRepository.statTotalSize(bucketId)).orElse(0L),
                "totalCount", Optional.ofNullable(entryAttachmentRepository.statTotalCount(bucketId)).orElse(0L));

        return CompletableFuture.completedFuture(stat);

    }
    public Map<String, Object> getStatByAppId(Long appId) {
        return Map.of("typeCount", Optional.ofNullable(entryAttachmentRepository.statCountByFileTypeByAppId(appId)).orElse(List.of()),
                "typeSize", Optional.ofNullable(entryAttachmentRepository.statSizeByFileTypeByAppId(appId)).orElse(List.of()),
                "labelCount", Optional.ofNullable(entryAttachmentRepository.statCountByItemLabelByAppId(appId)).orElse(List.of()),
                "labelSize", Optional.ofNullable(entryAttachmentRepository.statSizeByItemLabelByAppId(appId)).orElse(List.of()),
                "totalSize", Optional.ofNullable(entryAttachmentRepository.statTotalSizeByAppId(appId)).orElse(0L),
                "totalCount", Optional.ofNullable(entryAttachmentRepository.statTotalCountByAppId(appId)).orElse(0L));
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
        model.put("fileUrl", IO_BASE_DOMAIN + "/api/bucket/zip-download/" + filename);


        return CompletableFuture.completedFuture(model);
    }

    @Transactional
    public Map<String, Object> deleteFile(Long id) {

        Map<String, Object> data = new HashMap<>();
        EntryAttachment entryAttachment = entryAttachmentRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("EntryAttachment","id",id));

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


    @Transactional
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

    @Async("asyncExec")
    public CompletableFuture<List<Map<String, String>>> avLogList(Long bucketId){
        File dir = new File(Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + bucketId);

        File[] foundFiles = dir.listFiles((dir1, name) -> name.startsWith("av-scan-"));

        if (foundFiles==null){
            return CompletableFuture.completedFuture(List.of());
        }

        return CompletableFuture.completedFuture(Arrays.stream(foundFiles)
                .map(f->Map.of("fileName",f.getName(),"fileUrl",f.getAbsolutePath()))
                .toList());

    }


    @Transactional
    public Map<String, Object> scanBucketById(Long bucketId, OutputStream out) {
        Bucket bucket = bucketRepository.findById(bucketId).orElseThrow(()->new ResourceNotFoundException("Bucket","id", bucketId));
        return scanBucket(bucket, out);
    }


    @Transactional
    @Async("asyncExec")
    public Map<String, Object> scanBucket(Bucket bucket, OutputStream out) {

        final String bucketPath = Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + bucket.getId();
        File dir = new File(bucketPath);
        dir.mkdirs();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuuMMdd-HHmm");
        Path bucketLog = Paths.get(bucketPath + "/av-scan-" + dtf.format(LocalDateTime.now()) + ".log");

        try (
                BufferedWriter logWriter = Files.newBufferedWriter(bucketLog, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                out
        ) {

            final long start = System.nanoTime();

            log(logWriter, out, "⏱ Scan start (" + bucket.getName() + "): " + start);

            try (Stream<EntryAttachment> eaList = entryAttachmentRepository.findByBucketId(bucket.getId(), "%")) {

                eaList.forEach(ea -> {
                    entityManager.detach(ea);

                    String originalFilePath = bucketPath + "/" + ea.getFileUrl();
                    try {
                        log(logWriter, out, "🔍 Scanning file: " + originalFilePath);

                        Boolean isSafe = clamavService.scanFile(ea.getFileUrl(), originalFilePath);

                        String status;
                        String msg;

                        if (Boolean.FALSE.equals(isSafe)) {
                            status = "FOUND";
                            msg = "❌ ClamAV: Threat Found! File " + originalFilePath + " may be compromised.";
                            log(logWriter, out, msg);
                        } else {
                            status = "OK";
                            msg = "✅ ClamAV: File safe! " + originalFilePath;
                            log(logWriter, out, msg);
                        }

                        // Only update DB if values changed → huge performance improvement
                        if (!status.equals(ea.getSStatus()) || !msg.equals(ea.getSMessage())) {
                            entryAttachmentRepository.updateSMessage(
                                ea.isSuccess(),
                                ea.getMessage(),
                                status,
                                msg,
                                ea.getId()
                            );
                        }

                    } catch (Exception scanError) {
                        log(logWriter, out,
                                "⛔ ERROR scanning " + originalFilePath + ": " + scanError.getMessage());
                    }

                });
            }

            long end = System.nanoTime();
            long durationMs = (end - start) / 1_000_000;

            log(logWriter, out, "⏱ Scan end (" + bucket.getName() + "): " + end);
            log(logWriter, out, "⏱ Scan duration (" + bucket.getName() + "): " + durationMs + "ms");

        } catch (Exception e) {
            System.out.println("⛔ Error creating AV scan log: " + e.getMessage());
        }

        return null;
    }

    /** Unified logger: file + outputstream + console */
    private void log(BufferedWriter writer, OutputStream out, String message) {
        try {
            writer.write(message);
            writer.newLine();
            out.write((message + "\n").getBytes());
            System.out.println(message);
        } catch (IOException e) {
            throw new RuntimeException("Logger failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, Object> quarantine(Long id) {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuuMMdd-HHmm");
        LocalDateTime localDate = LocalDateTime.now();

        Map<String, Object> data = new HashMap<>();
        EntryAttachment entryAttachment = entryAttachmentRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("EntryAttachment","id",id));

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        if (entryAttachment.getBucketId() != null) {
            destStr += "bucket-" + entryAttachment.getBucketId() + "/";
        }

        File dir = new File(destStr);
        dir.mkdirs();

        File dest = new File(destStr + entryAttachment.getFileUrl());
        File rename = new File(destStr + entryAttachment.getFileUrl()+"-AV-REVOKED-"+dtf.format(localDate));
        data.put("success", dest.renameTo(rename));

        entryAttachment.setSStatus("VAULT");
        entryAttachment.setSMessage("✅ ClamAV: File quarantined!: The file "+entryAttachment.getFileUrl()+" has been quarantined.");

        entryAttachmentRepository.save(entryAttachment);

        return data;
    }



    @Scheduled(cron = "0 0/10 * * * ?") //0 */1 * * * *
    public Map<String, Object> runSchedule() {

        Calendar now = Calendar.getInstance();

        String clock = String.format("%02d%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        int day = now.get(Calendar.DAY_OF_WEEK); // sun=1, mon=2, tues=3,wed=4,thur=5,fri=6,sat=7
        int date = now.get(Calendar.DAY_OF_MONTH);
        int month = now.get(Calendar.MONTH); // 0-based month, ie: Jan=0, Feb=1, March=2

        bucketRepository.findScheduledByClock(clock).forEach(bucket -> {
            if ("daily".equals(bucket.getFreq()) ||
                    ("weekly".equals(bucket.getFreq()) && bucket.getDayOfWeek() == day) ||
                    ("monthly".equals(bucket.getFreq()) && bucket.getDayOfMonth() == date) ||
                    ("yearly".equals(bucket.getFreq()) && bucket.getMonthOfYear() == month && bucket.getDayOfMonth() == date)
            ) {
                scanBucket(bucket, System.out);
            }
        });
        return null;
    }


    public Map<String, Object> info() {
        return Map.of("avEnabled",clamavService.isEnabled());
    }




}
