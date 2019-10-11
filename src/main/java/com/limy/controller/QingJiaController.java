package com.limy.controller;

import com.alibaba.fastjson.JSON;
import com.limy.service.QingJiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;

@RestController
@RequestMapping("qingjia")
public class QingJiaController {

    @Autowired
    private QingJiaService qingJiaService;

    //开启流程实例
    @RequestMapping(value = "start", method = RequestMethod.GET)
    public String startProcessInstance() {
        return qingJiaService.start();
    }

    //提交请假申请
    @RequestMapping(value = "insert/{orderId}/{day}/{pid}", method = RequestMethod.GET)
    public void insert(@PathVariable int orderId, @PathVariable int day, @PathVariable String pid) {
        qingJiaService.insert(orderId,pid,day);
    }

    //进行认领操作
    @RequestMapping(value = "doAudit/{user}/{pid}", method = RequestMethod.GET)
    public void doAudit(@PathVariable String user, @PathVariable String pid) {
        qingJiaService.doAudit(user,pid);
    }

    //进行审批操作
    @RequestMapping(value = "audit/{user}/{auditFlag}/{pid}", method = RequestMethod.GET)
    public void complete(@PathVariable String user, @PathVariable Boolean auditFlag, @PathVariable String pid) {
        qingJiaService.audit(user,auditFlag,pid);
    }

    //获得用户可办理任务(被签收的任务无法查询)
    @RequestMapping(value = "getUserTask/{user}", method = RequestMethod.GET)
    public String getUserTask(@PathVariable String user) {
        return JSON.toJSONString(qingJiaService.getTasks(user));
    }

    //获取用户待办任务
    @RequestMapping(value = "getClaimTask/{user}", method = RequestMethod.GET)
    public String getclaimTasks(@PathVariable String user) {
        return JSON.toJSONString(qingJiaService.getclaimTasks(user));
    }

    //获取当前节点办理人
    @RequestMapping(value = "getTaskUsers/{pid}", method = RequestMethod.GET)
    public void getTaskUsers(@PathVariable String pid) {
        qingJiaService.getTaskUsers(pid);
    }


    //获取当前节点名称
    @RequestMapping(value = "getName/{pid}", method = RequestMethod.GET)
    public String getTasks(@PathVariable String pid) {
        return qingJiaService.getName(pid);
    }

    //获取当前审批进行到了哪一步，图片
    @RequestMapping(value = "image/{pid}", method = RequestMethod.GET)
    public void image(HttpServletResponse response, @PathVariable String pid) {
        try {
            InputStream is = qingJiaService.getDiagram(pid);
            if (is == null){
                return;
            }
            response.setContentType("image/png");

            BufferedImage image = ImageIO.read(is);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image, "png", out);

            is.close();
            out.close();
        } catch (Exception e) {
            System.out.println("查看流程图失败");
            e.printStackTrace();
        }
    }

}