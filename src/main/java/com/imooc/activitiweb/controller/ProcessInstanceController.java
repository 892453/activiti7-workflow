package com.imooc.activitiweb.controller;

import com.imooc.activitiweb.SecurityUtil;
import com.imooc.activitiweb.util.AjaxResponse;
import com.imooc.activitiweb.util.GlobalConfig;
import com.imooc.activitiweb.pojo.UserInfoBean;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/processInstance")
public class ProcessInstanceController {

    @Autowired
    private ProcessRuntime processRuntime;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private SecurityUtil securityUtil;


    /**
     * 查询【流程实例】
     * @param userInfoBean
     * @return
     */
    @GetMapping(value = "/getInstances")
    public AjaxResponse getInstances(@AuthenticationPrincipal UserInfoBean userInfoBean) {
        Page<ProcessInstance> processInstances = null;
        try {
            //测试用写死的用户POSTMAN测试用；生产场景已经登录，在processDefinitions中可以获取到当前登录用户的信息
            if (GlobalConfig.Test) {
                securityUtil.logInAs("wukong");
            }
            else {
              //securityUtil.logInAs(userInfoBean.getUsername());//这句不需要
                securityUtil.logInAs(SecurityContextHolder.getContext().getAuthentication().getName());
             }
            // 为了获取流程定义id，特别添加的
            List<ProcessDefinition> pro_def_list = repositoryService.createProcessDefinitionQuery().list();

            processInstances=processRuntime.processInstances(Pageable.of(0,  50));
            System.out.println("流程实例数量： " + processInstances.getTotalItems());
            List<ProcessInstance> list = processInstances.getContent();
            //list.sort((y,x)->x.getProcessDefinitionVersion()-y.getProcessDefinitionVersion());
            list.sort((y,x)->x.getStartDate().toString().compareTo(y.getStartDate().toString()));

            List<HashMap<String, Object>> listMap= new ArrayList<HashMap<String, Object>>();
            for(ProcessInstance pi:list){
                HashMap<String, Object> hashMap = new HashMap<>();
                System.out.println("getId："+pi.getId());
                System.out.println("getName："+pi.getName());
                System.out.println("getStatus："+pi.getStatus());
                System.out.println("getProcessDefinitionId："+pi.getProcessDefinitionId());
                System.out.println("getProcessDefinitionKey："+pi.getProcessDefinitionKey());
                System.out.println("getStartDate："+pi.getStartDate());

                hashMap.put("id",pi.getId());
                hashMap.put("name",pi.getName());
                // 通过pi.getProcessDefinitionId()获取的流程实例id不对，所以采用pd.getDeploymentId()来获取
                for (ProcessDefinition pd : pro_def_list) {
                    String pdid = pd.getDeploymentId().toString();
                    String ResourceName = pd.getResourceName();
                    if(pi.getProcessDefinitionId().endsWith(pdid.substring(10))){
                        System.out.println("ProcessDefinitionId:"+pdid);
                        hashMap.put("processDefinitionId2",pdid);
                        hashMap.put("ResourceName",ResourceName);
                    }
                }
                hashMap.put("processDefinitionId",pi.getProcessDefinitionId());
                hashMap.put("processDefinitionKey",pi.getProcessDefinitionKey());
                hashMap.put("initiator",pi.getInitiator());
                hashMap.put("startDate",pi.getStartDate());
                hashMap.put("businessKey",pi.getBusinessKey());
                hashMap.put("status",pi.getStatus());
                hashMap.put("processDefinitionVersion",pi.getProcessDefinitionVersion());
                listMap.add(hashMap);
            }

            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(),listMap);
        } catch (Exception e) {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取流程实例失败", e.toString());
        }


    }


    /**
     * 启动流程实例
     * @param processDefinitionKey
     * @param instanceName
     * @param instanceVariable
     * @return
     */
    @GetMapping(value = "/startProcess")
    public AjaxResponse startProcess(@RequestParam("processDefinitionKey") String processDefinitionKey,
                                     @RequestParam("instanceName") String instanceName,
                                     @RequestParam("instanceVariable") String instanceVariable) {
        try {
            if (GlobalConfig.Test) {
                securityUtil.logInAs("bajie");
            }else{
                securityUtil.logInAs(SecurityContextHolder.getContext().getAuthentication().getName());
            }

            ProcessInstance processInstance = processRuntime.start(ProcessPayloadBuilder
                    .start()
                    .withProcessDefinitionKey(processDefinitionKey)
                    .withName(instanceName)
                    .withVariable("content", instanceVariable)
                    .withVariable("参数2", "参数2的值")
                    .withBusinessKey("自定义BusinessKey")
                    .build());
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(), processInstance.getName()+"；"+processInstance.getId());
        } catch (Exception e) {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "创建流程实例失败", e.toString());
        }
    }

    /**
     * 删除流程实例
     * @param instanceID
     * @return
     */
    @GetMapping(value = "/deleteInstance")
    public AjaxResponse deleteInstance(@RequestParam("instanceID") String instanceID) {
        try {
            if (GlobalConfig.Test) {
                securityUtil.logInAs("wukong");
            }

            ProcessInstance processInstance = processRuntime.delete(ProcessPayloadBuilder
                    .delete()
                    .withProcessInstanceId(instanceID)
                    .build()
            );
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(), processInstance.getName());
        }
     catch(Exception e)
        {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "删除流程实例失败", e.toString());
        }

    }

    /**
     * 挂起流程实例
     * @param instanceID
     * @return
     */
    @GetMapping(value = "/suspendInstance")
    public AjaxResponse suspendInstance(@RequestParam("instanceID") String instanceID) {

        try {
            if (GlobalConfig.Test) {
                securityUtil.logInAs("wukong");
            }

            ProcessInstance processInstance = processRuntime.suspend(ProcessPayloadBuilder
                    .suspend()
                    .withProcessInstanceId(instanceID)
                    .build()
            );
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(), processInstance.getName());
        }
        catch(Exception e)
        {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "挂起流程实例失败", e.toString());
        }
    }

    /**
     * 激活流程实例
     * @param instanceID
     * @return
     */
    @GetMapping(value = "/resumeInstance")
    public AjaxResponse resumeInstance(@RequestParam("instanceID") String instanceID) {

        try {
            if (GlobalConfig.Test) {
                securityUtil.logInAs("wukong");
            }

            ProcessInstance processInstance = processRuntime.resume(ProcessPayloadBuilder
                    .resume()
                    .withProcessInstanceId(instanceID)
                    .build()
            );
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(), processInstance.getName());
        }
        catch(Exception e)
        {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "激活流程实例失败", e.toString());
        }
    }


    /**
     * 查询流程参数
     * @param instanceID
     * @return
     */
    @GetMapping(value = "/variables")
    public AjaxResponse variables(@RequestParam("instanceID") String instanceID) {
        try {
            if (GlobalConfig.Test) {
                securityUtil.logInAs("wukong");
            }
            List<VariableInstance> variableInstance = processRuntime.variables(ProcessPayloadBuilder
                    .variables()
                    .withProcessInstanceId(instanceID)
                    .build());

            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(), variableInstance);
        }
        catch(Exception e)
        {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取流程参数失败", e.toString());
        }
    }





    
}
