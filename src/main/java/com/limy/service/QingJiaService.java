package com.limy.service;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: Limy
 * @create: 2019/08/26 14:02
 * @description: ${description}
 */
@Service(value = "qingJiaService")
public class QingJiaService{

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ProcessEngine processEngine;

    //开始请假流程
    public String start() {
        ProcessInstance first = runtimeService.startProcessInstanceByKey("QingJia");
        System.out.println("开始请假流程，当前ID=" + first.getId());
        return first.getId();
    }

    //提交审批申请
    public void insert(int orderId, String pid, int day) {
        Task task = taskService.createTaskQuery().processInstanceId(pid).singleResult();
        System.out.println("当前流程节点：" + task.getName());
        System.out.println("写入请假申请表");
        //设置流程参数：请假天数和表单ID
        //流程引擎会根据请假天数days>3判断流程走向
        //orderId是用来将流程数据和表单数据关联起来
        Map<String, Object> args = new HashMap<>();
        args.put("day", day);
        args.put("orderId", orderId);
        //完成请假申请任务
        taskService.complete(task.getId(), args);
    }

    //认领任务
    public void doAudit(String user, String pid) {
        //查询当前审批节点
        Task task = taskService.createTaskQuery().processInstanceId(pid).singleResult();
        //设置审批任务的执行人
        taskService.claim(task.getId(), user);
        System.out.println("认领成功，等待审批");
    }

    //进行审批操作
    public void audit(String user, Boolean auditFlag, String pid) {
        //查询当前审批节点
        Task task = taskService.createTaskQuery().processInstanceId(pid).singleResult();
        //获取当前节点审批人List
        List<String> users = getTaskUsers(task.getId());
        System.out.println("当前节点办理人为：" + users.toString());
        if (users != null && users.size() > 0) {
            if (!users.contains(user)) {
                System.out.println("该审批人没有当前节点审批权限");
                return;
            }
        }
        if (auditFlag) {//审批通过
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("auditFlag", auditFlag);
            //完成任务
            taskService.setAssignee(task.getId(), user);
            taskService.complete(task.getId(), map);

            //判断流程是否结束
            Task endtask = taskService.createTaskQuery().processInstanceId(pid).singleResult();
            if (endtask == null) {
                System.out.println("该节点为最后一个节点，审批完成");
            } else {
                System.out.println("审批通过，进入下一个流程");
            }
        } else {
            //审批不通过，结束流程
            runtimeService.deleteProcessInstance(pid, "审批不通过，结束流程");
            System.out.println("审批不通过，结束流程");
        }

    }


    //获得用户可办理任务(被签收的任务无法查询)
    public List<Task> getTasks(String user) {
        List<Task> list = taskService.createTaskQuery().taskCandidateUser(user).list();
        for (Task task : list) {
            System.out.println(task);
        }
        return list;
    }

    //获取用户待办任务
    public List<Task> getclaimTasks(String user) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(user).list();
        for (Task task : tasks) {
            System.out.println(task);
        }
        return tasks;
    }

    //获取当前节点的办理人
    public List<String> getTaskUsers(String taskId) {
        //获取当前节点
        List<IdentityLink> identityLinksForTask = taskService.getIdentityLinksForTask(taskId);
        return identityLinksForTask.stream().map(n -> n.getUserId()).collect(Collectors.toList());
    }

    //当前节点名称
    public String getName(String pid) {
        //查询当前审批节点
        Task task = taskService.createTaskQuery().processInstanceId(pid).singleResult();
        System.out.println("当前流程节点：" + task.getName());
        return task.getName();
    }

    //获取流程图图片
    public InputStream getDiagram(String processInstanceId) {
        //获得流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        String processDefinitionId = StringUtils.EMPTY;
        if (processInstance == null) {
            //查询已经结束的流程实例
            HistoricProcessInstance processInstanceHistory =
                    historyService.createHistoricProcessInstanceQuery()
                            .processInstanceId(processInstanceId).singleResult();
            if (processInstanceHistory == null)
                return null;
            else
                processDefinitionId = processInstanceHistory.getProcessDefinitionId();
        } else {
            processDefinitionId = processInstance.getProcessDefinitionId();
        }

        //使用宋体
        String fontName = "宋体";
        //获取BPMN模型对象
        BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
        //获取流程实例当前的节点，需要高亮显示
        List<String> currentActs = Collections.EMPTY_LIST;
        if (processInstance != null)
            currentActs = runtimeService.getActiveActivityIds(processInstance.getId());

        return processEngine.getProcessEngineConfiguration()
                .getProcessDiagramGenerator()
                .generateDiagram(model, "png", currentActs, new ArrayList<String>(),
                        fontName, fontName, fontName, null, 1.0);
    }

    //ServiceTask执行任务: bpmn文件中 Type 为 Expression ， vale 为 ${qingJiaService.doservice(execution)}
    public void doservice(DelegateExecution execution) {
        System.out.println("----------------------执行了doservice任务----------------------");
        Boolean bool = (Boolean) execution.getVariable("auditFlag");
        if (bool) {
            System.out.println("申请通过，执行doservice");
        } else {
            System.out.println("申请不通过，执行doservice");
        }
    }

    //设置节点审批人列表
    public List<String> findAuditUserList(DelegateExecution execution) {
        System.out.println("----------------------设置下一节点的审批人----------------------");
        List<String> list;
        int day = (int) execution.getVariable("day");
        //执行某种判断逻辑
        if (day >= 1 && day <= 3) {
            list = Arrays.asList("组长1", "组长2");
        } else {
            list = Arrays.asList("经理1", "经理2");
        }
        System.out.println("----------------------设置下一节点的审批人:" + list.toString() + "----------------------");
        return list;
    }

}
