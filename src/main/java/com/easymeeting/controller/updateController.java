package com.easymeeting.controller;

import com.easymeeting.entity.config.AppConfig;
import com.easymeeting.entity.constants.Constants;
import com.easymeeting.entity.enums.ResponseCodeEnum;
import com.easymeeting.entity.po.AppUpdate;
import com.easymeeting.entity.vo.AppUpdateVo;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.service.AppUpdateService;
import com.easymeeting.utils.CopyTools;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import io.netty.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController("updateController")
@RequestMapping("/update")
public class updateController extends ABaseController{
    @Resource
    private AppConfig appConfig;
    @Resource
    private AppUpdateService appUpdateService;
    @RequestMapping("/checkVersion")
public ResponseVO checkVersion(@NotEmpty String appVersion,@NotEmpty String uid) {
        AppUpdate appUpdate = appUpdateService.selectLatestUpdate(appVersion, uid);
        AppUpdateVo appUpdateVo = CopyTools.copy(appUpdate, AppUpdateVo.class);
        File file = new File(appConfig.getProjectFolder() + Constants.APP_UPDATE_FOLDER + "/" + appUpdate.getId() + Constants.APP_EXE_SUFFIX);
        if (file.exists()) {
            appUpdateVo.setSize(file.length());
        }else {
            appUpdateVo.setSize(0L);
        }
        if (!StringUtils.isEmpty(appUpdate.getUpdateDesc())){
            List<String> list = Arrays.asList(appUpdate.getDescSplit());
            appUpdateVo.setUpdateList(list);
        }
        String fileName = Constants.APP_NAME+appUpdate.getId()+Constants.APP_EXE_SUFFIX;
        appUpdateVo.setFileName(fileName);
        return getSuccessResponseVO(appUpdateVo);
}

    @RequestMapping("/downloadApp")
    public void downloadApp(@NotNull Integer id, HttpServletResponse response) {
        AppUpdate appUpdateById = this.appUpdateService.getAppUpdateById(id);
        if (appUpdateById == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        File file = new File(appConfig.getProjectFolder() + Constants.APP_UPDATE_FOLDER + appUpdateById.getId() + Constants.APP_EXE_SUFFIX);
        if (!file.exists()) {
            return;
        }
        response.setContentType("application/x-msdownload; charset=utf-8");
        response.setHeader("Content-Disposition","attachment;");
        response.setContentLengthLong(file.length());
        try(ServletOutputStream outputStream = response.getOutputStream(); FileInputStream fileInputStream = new FileInputStream(file)){
            byte[] bytes = new byte[1024];
            int len=0;
            while((len = fileInputStream.read(bytes))!=-1){
             outputStream.write(bytes,0,len); //循环写入 repsonse bytes 为当前循环从file读取的字节 0 为bytes写入repsonse的初始位置；
            }
            outputStream.flush();
        }catch (Exception e){
            log.error("读取文件异常");
        }


    }

}
