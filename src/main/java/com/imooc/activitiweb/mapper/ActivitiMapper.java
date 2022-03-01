package com.imooc.activitiweb.mapper;



import com.imooc.activitiweb.pojo.Act_ru_task;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;


@Mapper
@Component
public interface ActivitiMapper {

    @Select("select NAME_,TASK_DEF_KEY_ from act_ru_task")
    List<Act_ru_task> selectName();

    @Insert(
            "<script> insert into formdata(PROC_DEF_ID_,PROC_INST_ID_,FORM_KEY_,Control_ID_,Control_VALUE_)" +
                    "values" +
                    "<foreach collection=\"maps\" item=\"formData\" index=\"index\" separator=\",\">" +
                    "(" +
                    "#{formData.PROC_DEF_ID_,jdbcType=VARCHAR},#{formData.PROC_INST_ID_,jdbcType=VARCHAR},#{formData.FORM_KEY_,jdbcType=VARCHAR}," +
                    "#{formData.Control_ID_,jdbcType=VARCHAR},#{formData.Control_VALUE_,jdbcType=VARCHAR}" +
                    ")</foreach>"+
            "</script>"
    )
    int insertFormData(@Param("maps")List<HashMap<String,Object>> maps);

    //从数据表formdata里按照【流程实例id  PROC_INST_ID】查询【控件id和值  Control_ID_,Control_VALUE_】，返回给前端表单渲染
    //TaskController的 /task/formDataShow  里用到
    @Select("select Control_ID_,Control_VALUE_ from formdata where PROC_INST_ID_ = #{PROC_INST_ID}")
    List<HashMap<String,Object>> selectFormData(@Param("PROC_INST_ID") String PROC_INST_ID);

    //获取用户名
    //UserContorller的 user/getUsers 用到
    @Select("SELECT name,username from user")
    List<HashMap<String,Object>> selectUser();
}
