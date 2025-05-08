package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.EmailTemplate;
import com.benzourry.leap.model.Schedule;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.ScheduleRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@EnableScheduling
public class ScheduleService {

    public ScheduleRepository scheduleRepository;

    public EntryService entryService;

    public EmailTemplateService emailTemplateService;

    public AppRepository appRepository;

    public ScheduleService(ScheduleRepository scheduleRepository,
                           EntryService entryService,
                           EmailTemplateService emailTemplateService,
                           AppRepository appRepository){
        this.scheduleRepository = scheduleRepository;
        this.entryService = entryService;
        this.emailTemplateService = emailTemplateService;
        this.appRepository = appRepository;
    }


    public Schedule save(Long appId,Schedule schedule){
        App app = appRepository.getReferenceById(appId);
        schedule.setApp(app);
        return scheduleRepository.save(schedule);
    }

    public List<Schedule> findByAppId(Long appId){
        return scheduleRepository.findByAppId(appId);
    }

    public void delete(Long id){
        scheduleRepository.deleteById(id);
    }

    public void deleteByAppId(Long appId){
        scheduleRepository.deleteByAppId(appId);
    }

    @Scheduled(cron = "0 0/10 * * * ?") //0 */1 * * * *
    public Map<String, Object> runSchedule(){

        Calendar now = Calendar.getInstance();

        String clock = String.format("%02d%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        int day = now.get(Calendar.DAY_OF_WEEK); // sun=1, mon=2, tues=3,wed=4,thur=5,fri=6,sat=7
        int date = now.get(Calendar.DAY_OF_MONTH);
        int month = now.get(Calendar.MONTH); // 0-based month, ie: Jan=0, Feb=1, March=2

        Map filters = new HashMap();

        scheduleRepository.findByEnabledAndClock(clock).stream()
        .forEach(s->{
            // must compare with clock
            if ("daily".equals(s.getFreq()) ||
                    ("weekly".equals(s.getFreq()) && s.getDayOfWeek() == day) ||
                    ("monthly".equals(s.getFreq()) && s.getDayOfMonth() == date) ||
                    ("yearly".equals(s.getFreq()) && s.getMonthOfYear() == month && s.getDayOfMonth() == date)
            ) {
                System.out.println("---- dlm forEach clock:"+ s.getName());
                EmailTemplate mailer = emailTemplateService.getEmailTemplate(s.getMailerId());
                try {
                    entryService.blastEmailByDataset(s.getDatasetId(),null, s.getEmail(),filters,"AND",mailer,null,null, null,null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return null;
    }

    public Schedule getSchedule(long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Schedule","id",id));
    }
}
