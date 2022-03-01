package com.imooc.activitiweb.controller;

import com.imooc.activitiweb.SecurityUtil;
import com.imooc.activitiweb.mapper.ActivitiMapper;
import com.imooc.activitiweb.util.AjaxResponse;
import com.imooc.activitiweb.util.GlobalConfig;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.bpmn.model.FormProperty;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.RepositoryService;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/task")
public class TaskController {
    @Autowired
    private TaskRuntime taskRuntime;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private ProcessRuntime processRuntime;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ActivitiMapper mapper;

    /**
     * 获取我的代办任务
     * @return
     */
    @GetMapping(value = "/getTasks")
    public AjaxResponse getTasks() {
        try {
            if (GlobalConfig.Test) {
                securityUtil.logInAs("wukong");
            }
            Page<Task> tasks = taskRuntime.tasks(Pageable.of(0, 100));

            List<HashMap<String, Object>> listMap = new ArrayList<HashMap<String, Object>>();

            for (Task tk : tasks.getContent()) {
                ProcessInstance processInstance = processRuntime.processInstance(tk.getProcessInstanceId());
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("ID", tk.getId());
                hashMap.put("Name", tk.getName());
                hashMap.put("Status", tk.getStatus());
                hashMap.put("CreatedDate", tk.getCreatedDate());
                if(tk.getAssignee() == null){//执行人，null时前台显示未拾取
                    hashMap.put("Assignee", "待拾取任务");
                }else {
                    hashMap.put("Assignee", tk.getAssignee());//
                }

                hashMap.put("InstanceName", processInstance.getName());
                listMap.add(hashMap);
            }

            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(), listMap);


        } catch (Exception e) {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取我的待办任务失败", e.toString());
        }
    }

    /**
     * 完成待办任务
     * @param taskID
     * @return
     */
    @GetMapping(value = "/completeTask")
    public AjaxResponse completeTask(@RequestParam("taskID") String taskID) {
        try {
            if (GlobalConfig.Test) {
                securityUtil.logInAs("bajie");
            }
            Task task = taskRuntime.task(taskID);

            //此任务没有负责人，则需要先拾取任务
            if (task.getAssignee() == null) {
                taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(task.getId()).build());
            }
            //再作为负责人完成此任务
            taskRuntime.complete(TaskPayloadBuilder.complete().withTaskId(task.getId()).build());


            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(), null);
        } catch (Exception e) {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "完成失败", e.toString());
        }
    }

    /**
     * 渲染动态表单
     * @param taskID
     * @return
     */
    @GetMapping(value = "/formDataShow")
    public AjaxResponse formDataShow(@RequestParam("taskID") String taskID) {
        try {
            if (GlobalConfig.Test) {
                securityUtil.logInAs("bajie");
            }
            Task task = taskRuntime.task(taskID);

            //构建表单历史控件数据
            HashMap<String,String> ControlListMap = new HashMap<String,String>();
            //读取数据库本 【流程实例】 下所有表单数据
            List<HashMap<String,Object>> tempControlList = mapper.selectFormData(task.getProcessInstanceId());
            for(HashMap ls : tempControlList){
                ControlListMap.put(ls.get("Control_ID_").toString(),ls.get("Control_VALUE_").toString());
            }

            UserTask userTask = (UserTask) repositoryService.getBpmnModel(task.getProcessDefinitionId())
                    .getFlowElement(task.getFormKey());

            //无表单任务时，用户点击”办理“，即可直接办理完成任务
            if(userTask==null){
                return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                        GlobalConfig.ResponseCode.SUCCESS.getDesc(), "无表单");
            }

            List<FormProperty> formProperties = userTask.getFormProperties();
            List<HashMap<String,Object>> listmap = new ArrayList<>();
            System.out.println("formProperties:"+formProperties.toArray());
            for(FormProperty fp:formProperties){
                String[] splitFP = fp.getId().split("-_!");
                System.out.println("splitFP:"+splitFP+splitFP[0]+"  "+splitFP[1]+"   "+splitFP[2]+"   "+splitFP[3]+"   "+splitFP[4]);
                HashMap<String,Object> hashMap = new HashMap<>();
                hashMap.put("formId",splitFP[0]);
                hashMap.put("controlType",splitFP[1]);
                hashMap.put("controlLable",splitFP[2]);
//                hashMap.put("controlDefValue",splitFP[3]);
                if(splitFP[3].startsWith("FormProperty")){
                    if(ControlListMap.containsKey(splitFP[3])) {
                        hashMap.put("controlDefValue", ControlListMap.get(splitFP[3]));
                    }else{
                        hashMap.put("controlDefValue", "读取失败，检查"+splitFP[0]+"配置");
                    }
                }else{
                    hashMap.put("controlDefValue",splitFP[3]);
                }
                hashMap.put("controlParam",splitFP[4]);
                listmap.add(hashMap);
            }
            System.out.println("listmap:"+listmap);

            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(), listmap);
        } catch (Exception e) {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "任务表单渲染失败", e.toString());
        }
    }


    /**
     * 保存动态表单
     * @param taskID
     * @return
     */
    @PostMapping (value = "/formDataSave")
    public AjaxResponse formDataSave(@RequestParam("taskID") String taskID
                                    ,@RequestParam("formData") String fromData) {
        try {
            if (GlobalConfig.Test) {
                securityUtil.logInAs("bajie");
            }
            Task task = taskRuntime.task(taskID);
            UserTask userTask = (UserTask) repositoryService.getBpmnModel(task.getProcessDefinitionId())
                    .getFlowElement(task.getFormKey());

            HashMap<String,Object> variables = new HashMap<String,Object>();
            Boolean hasVariables = false;

            //前端传来的字符串拆分为控件组
            String[] formDataList = fromData.split("!_!");
            List<HashMap<String,Object>> listmap = new ArrayList<>();
            for(String controlItem:formDataList){
                String[] formDataItem = controlItem.split("-_!");
                HashMap<String,Object> hashMap = new HashMap<>();
                hashMap.put("PROC_DEF_ID_",task.getProcessDefinitionId());
                hashMap.put("PROC_INST_ID_",task.getProcessInstanceId());
                hashMap.put("FORM_KEY_",task.getFormKey());
                hashMap.put("Control_ID_",formDataItem[0]);
                hashMap.put("Control_VALUE_",formDataItem[1]);
                hashMap.put("Control_PARAM_",formDataItem[2]);
                listmap.add(hashMap);


                //构建参数集合
                switch (formDataItem[2]){
                    case "f":
                        System.out.println("控件值不作为参数");
                        break;
                    case "s":
                        variables.put(formDataItem[0],formDataItem[1]);
                        hasVariables=true;
                        break;
                    case "t":
                        SimpleDateFormat timeformat = new SimpleDateFormat("YYYY-MM-dd HH:mm");
                        variables.put(formDataItem[0],timeformat.parse(formDataItem[1]));
                        hasVariables=true;
                        break;
                    case "b":
                        variables.put(formDataItem[0], BooleanUtils.toBoolean(formDataItem[1]));
                        hasVariables=true;
                        break;
                    default:
                        System.out.println("控件ID"+formDataItem[0]+"的参数： "+formDataItem[1]+" 不存在");
                }
            }//for循环结束

            if(hasVariables){
                //带参数完成任务
                taskRuntime.complete(TaskPayloadBuilder.complete()
                        .withTaskId(taskID)
                        .withVariables(variables)
                        .build()
                );
            }else{
                taskRuntime.complete(TaskPayloadBuilder.complete()
                        .withTaskId(taskID)
                        .build()
                );
            }

            int result = mapper.insertFormData(listmap);

            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(), listmap);
        } catch (Exception e) {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "任务表单保存失败", e.toString());
        }
    }
    //启动
    @GetMapping(value = "/startProcess4")
    public AjaxResponse startProcess3(@RequestParam("processDefinitionKey") String processDefinitionKey,
                                      @RequestParam("instanceName") String instanceName,
                                      @RequestParam("instanceVariable") String instanceVariable) {
        try {
            if (GlobalConfig.Test) {
                securityUtil.logInAs("wukong");
            }


/*            @RequestMapping("/approval_msg")
            @ResponseBody
            public JsonResponse approvalPass(String id,String msg){
                JsonResponse jsonResponse = new JsonResponse();

                if(StringUtil.isNotEmpty(msg)){
                    String str= msg.replace("\"", "");
                    taskService.setVariable(id,"msg",str);
                }
                taskService.complete(id);
                return jsonResponse;
            }*/

            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(), null);
        } catch (Exception e) {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "失败", e.toString());
        }
    }

}
