package com.easymeeting.controller;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.po.AppUpdate;
import com.easymeeting.entity.query.AppUpdateQuery;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.service.AppUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

@RestController
@Slf4j
@Validated
@RequestMapping("/admin")
public class AppUpdateController extends ABaseController{
@Resource
private AppUpdateService appUpdateService;
@RequestMapping("/loadUpdateList")
@globalInterceptor
public ResponseVO loadUpdateList(AppUpdateQuery query){
    query.setOrderBy("id desc");
    List<AppUpdate> listByParam = this.appUpdateService.findListByParam(query);
    return getSuccessResponseVO(listByParam);
}

    @RequestMapping("/saveUpdate")
    @globalInterceptor(checkAdmin = true)
    public ResponseVO saveUpdate(Integer id, @NotEmpty String version, @NotEmpty String updateDesc, @NotNull Integer fileType, String outerLink , MultipartFile file
                                 ) throws IOException {
        AppUpdate appUpdate = new AppUpdate();
        appUpdate.setId(id);
        appUpdate.setVersion(version);
        appUpdate.setUpdateDesc(updateDesc);
        appUpdate.setFileType(fileType);
        appUpdate.setOuterLink(outerLink);
        this.appUpdateService.saveUpdate(appUpdate,file);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/delUpdate")
    @globalInterceptor(checkAdmin = true)
    public ResponseVO delUpdate(@NotNull Integer id) throws IOException {
        this.appUpdateService.deleteAppUpdateById(id);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/postUpdate")
    @globalInterceptor(checkAdmin = true)
    public ResponseVO postUpdate(@NotNull Integer id,Integer status ,String grayScaleUid) throws IOException {
        this.appUpdateService.postUpdate(id,status,grayScaleUid);
        return getSuccessResponseVO(null);
    }






}
