//package com.benzourry.leap.controller;
//
//import com.benzourry.leap.service.DeepLearnService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.io.IOException;
//
//@RestController
//@RequestMapping({"api/dl","api/public/dl"})
//public class DeepLearnController {
//
//    @Autowired
//    DeepLearnService deepLearnService;
//
//    @GetMapping("test")
//    public String test() throws IOException, InterruptedException {
//        return deepLearnService.createNet();
//    }
//    @GetMapping("predict")
//    public Object predict(@RequestParam double[] p){
//        return deepLearnService.predict(p[0],p[1],p[2],p[3]);
//    }
//}
