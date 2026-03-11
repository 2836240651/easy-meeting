package com.easymeeting.controller;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.config.AppConfig;
import com.easymeeting.entity.constants.Constants;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.DateTimePatternEnum;
import com.easymeeting.entity.enums.FileTypeEnum;
import com.easymeeting.entity.enums.ResponseCodeEnum;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.utils.DateUtil;
import com.easymeeting.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Date;

@RestController
@Slf4j
@Validated
@RequestMapping("/file")
public class FileController extends ABaseController {

  @Resource
  private AppConfig appConfig;
    @RequestMapping("/getAvatar")
    @globalInterceptor(checkLogin = false)
    public void getAvatar(HttpServletResponse response,String userId,@NotEmpty String token){
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo(token);
        if (tokenUserInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        String filePath = appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+Constants.FILE_FOLDER_AVATAR_NAME+userId+Constants.IMAGE_SUFFIX;
        response.setContentType("image/jpg");
        File file = new File(filePath);
        if (!file.exists()) {
            readLocalFile(response);
            return;
        }
        readFile(response,null,filePath,false);

    }
    private void readLocalFile(HttpServletResponse response) {
    response.setHeader("cache-control", "max-age="+30*24*60*60);
    response.setContentType("image/jpg");
        ClassPathResource classPathResource = new ClassPathResource(Constants.DEFAULT_AVATAR);
        try(        ServletOutputStream out = response.getOutputStream();
                    InputStream in = classPathResource.getInputStream();){
            byte[] byteAvatar = new byte[1024];
            int len =0;
            while((len=in.read(byteAvatar))!=-1){
                out.write(byteAvatar,0,len);
            }
            out.flush();
        }catch (Exception e){log.error(e.getMessage());}


    }
    @RequestMapping("/downloadFile")
    @globalInterceptor(checkLogin = false)
    public ResponseVO downloadFile(HttpServletResponse response,@NotNull Long messageId,@NotNull Long sendTime,
                                   @NotEmpty String token,@NotEmpty String suffix) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo(token);
        if (tokenUserInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        String month = DateUtil.format(new Date(sendTime), DateTimePatternEnum.YYYY_MM.getPattern());
        String filePath = appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+month+"/"+messageId+suffix;
        File file = new File(filePath);
        response.setContentType("application/x-msdownload; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;");
        response.setContentLengthLong(file.length());
        try(        FileInputStream in = new FileInputStream(file);
                    ServletOutputStream out = response.getOutputStream();){
            byte[] byteData = new byte[1024];
            int len = 0 ;
            while ((len=in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();
        }catch (Exception e) {
            log.error(e.getMessage());
        }
        return getSuccessResponseVO(null);
    }




    @RequestMapping("/getResource")
    @globalInterceptor
    public ResponseVO getResource(HttpServletResponse response, @NotEmpty String token, @RequestHeader(required = false,name = "range")String range
                                  , @NotNull Long messageId,
                                  @NotNull Long sendTime,
                                  @NotNull Integer fileType, Boolean thumbnail
    ) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo(token);
        if (tokenUserInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        String month= DateUtil.format(new Date(sendTime), DateTimePatternEnum.YYYY_MM.getPattern());
        FileTypeEnum fileTypeEnum = FileTypeEnum.getByType(fileType);
        String filePath = appConfig.getProjectFolder()+ Constants.FILE_FOLDER_FILE +month+"/"+messageId+fileTypeEnum.getSuffix();
        thumbnail = thumbnail ==null ? false:thumbnail;
        if (fileTypeEnum==null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        switch (fileTypeEnum){
            case IMAGE:
            response.setHeader("Cache-Control", "max-age="+30*24*60*60);
            response.setContentType("image/jpg");
            break;
        }
        readFile(response,range,filePath,thumbnail);


        return getSuccessResponseVO(null);
    }

    private void readFile(HttpServletResponse response, String range, String filePath, Boolean thumbnail) {
    filePath = thumbnail? StringTools.getImageThumbnail(filePath):filePath;
        File fileTemp = new File(filePath);
    try(        ServletOutputStream out = response.getOutputStream();){
        RandomAccessFile r = new RandomAccessFile(fileTemp, "r"); //只读模式
        long contentLength = r.length();
        int start = 0 ,end = 0;
        if (range !=null && range.startsWith("bytes=")){
            String[] values=range.split("=")[1].split("-");
            start = Integer.parseInt(values[0]);
            if (values.length>1) {
                end = Integer.parseInt(values[1]);
            }
        }


    int requestSize = 0 ;
        if (end!=0 && end>start){
            requestSize = end-start+1;
        }else {
            requestSize = Integer.MAX_VALUE;
        }
        byte[] buffer = new byte[4096];
        response.setHeader("Accept-Ranges","bytes");
        response.setHeader("Last-Modified",new Date().toString());
        //第一次请求只返回content length 来让客户端请求多次实际数据
        if (range ==null){
            response.setHeader("Content-Length",contentLength+"");
        }else {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            long requestStart=0,requestEnd=0;
            String[] ranges = range.split("=");
            if (ranges.length>1) {
                String[] rangeDatas = ranges[1].split("-");
                requestStart = Integer.parseInt(rangeDatas[0]);
                if (rangeDatas.length>1) {
                    requestEnd = Integer.parseInt(rangeDatas[1]);
                }
            }
            long length = 0;
            if (requestEnd >0){
                length = requestEnd-requestStart+1;
                response.setHeader("Content-Length",""+length);
                response.setHeader("Content-Range","bytes "+requestStart+"-"+requestEnd+"/"+contentLength);
            }else {
                length = contentLength-requestStart;
                response.setHeader("Content-Length",""+length);
                response.setHeader("Content-Range","bytes "+requestStart+"-"+(contentLength-1)+"/"+contentLength);
            }
        }
        int needSize = requestSize;
        r.seek(start);
        while (needSize>0){
            int len = r.read(buffer);
            if (needSize<buffer.length){
                out.write(buffer,0,needSize);
            }else {
                out.write(buffer,0,len);
                if (len<buffer.length){
                    break;
                }
            }
            needSize -= buffer.length;
        }
        r.close();
    }catch (Exception e){
       log.error("读取文件信息失败！");
    }

    }
}
