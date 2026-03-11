package com.easymeeting.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Date;
import javax.annotation.Resource;

import com.easymeeting.entity.config.AppConfig;
import com.easymeeting.entity.constants.Constants;
import com.easymeeting.entity.enums.AppUpdateStatusEnum;
import com.easymeeting.entity.enums.FileTypeEnum;
import com.easymeeting.entity.enums.ResponseCodeEnum;
import com.easymeeting.exception.BusinessException;
import io.netty.util.Constant;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easymeeting.entity.enums.PageSize;
import com.easymeeting.entity.query.AppUpdateQuery;
import com.easymeeting.entity.po.AppUpdate;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.query.SimplePage;
import com.easymeeting.mappers.AppUpdateMapper;
import com.easymeeting.service.AppUpdateService;
import com.easymeeting.utils.StringTools;
import org.springframework.web.multipart.MultipartFile;


/**
 *  业务接口实现
 */
@Service("appUpdateService")
public class AppUpdateServiceImpl implements AppUpdateService {

	@Resource
	private AppUpdateMapper<AppUpdate, AppUpdateQuery> appUpdateMapper;
    @Autowired
    private AppConfig appConfig;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<AppUpdate> findListByParam(AppUpdateQuery param) {
		return this.appUpdateMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(AppUpdateQuery param) {
		return this.appUpdateMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<AppUpdate> findListByPage(AppUpdateQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<AppUpdate> list = this.findListByParam(param);
		PaginationResultVO<AppUpdate> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(AppUpdate bean) {
		return this.appUpdateMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<AppUpdate> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.appUpdateMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<AppUpdate> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.appUpdateMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(AppUpdate bean, AppUpdateQuery param) {
		StringTools.checkParam(param);
		return this.appUpdateMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(AppUpdateQuery param) {
		StringTools.checkParam(param);
		return this.appUpdateMapper.deleteByParam(param);
	}

	/**
	 * 根据Id获取对象
	 */
	@Override
	public AppUpdate getAppUpdateById(Integer id) {
		return this.appUpdateMapper.selectById(id);
	}

	/**
	 * 根据Id修改
	 */
	@Override
	public Integer updateAppUpdateById(AppUpdate bean, Integer id) {
		return this.appUpdateMapper.updateById(bean, id);
	}

	/**
	 * 根据Id删除
	 */
	@Override
	public Integer deleteAppUpdateById(Integer id) {
		AppUpdate appUpdate = this.appUpdateMapper.selectById(id);
		if (appUpdate == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if (!AppUpdateStatusEnum.INIT.getStatus().equals(appUpdate.getStatus())) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		return this.appUpdateMapper.deleteById(id);
	}

	/**
	 * 根据Version获取对象
	 */
	@Override
	public AppUpdate getAppUpdateByVersion(String version) {
		return this.appUpdateMapper.selectByVersion(version);
	}

	/**
	 * 根据Version修改
	 */
	@Override
	public Integer updateAppUpdateByVersion(AppUpdate bean, String version) {
		return this.appUpdateMapper.updateByVersion(bean, version);
	}

	/**
	 * 根据Version删除
	 */
	@Override
	public Integer deleteAppUpdateByVersion(String version) {
		return this.appUpdateMapper.deleteByVersion(version);
	}

	@Override
	public void saveUpdate(AppUpdate appUpdate, MultipartFile file) throws IOException {
		FileTypeEnum fileTypeEnum = FileTypeEnum.getByType(appUpdate.getFileType());
		if (fileTypeEnum==null) {
		throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		if (appUpdate.getId()!=null){
			AppUpdate appUpdate1 = this.appUpdateMapper.selectById(appUpdate.getId());
			if (!appUpdate1.getStatus().equals(AppUpdateStatusEnum.INIT.getStatus())) {
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
		}

		SimplePage simplePage = new SimplePage(0, 1);
		AppUpdateQuery appUpdateQuery = new AppUpdateQuery();
		appUpdateQuery.setOrderBy("id desc");
		appUpdateQuery.setSimplePage(simplePage);
		List<AppUpdate> appUpdateList = this.appUpdateMapper.selectList(appUpdateQuery);
		if (!appUpdateList.isEmpty()) {
			Long dbVersion = Long.parseLong(appUpdateList.get(0).getVersion().replace(".",""));
			Long currentVersion = Long.parseLong(appUpdate.getVersion().replace(".",""));
			if (appUpdate.getId()==null&&dbVersion>currentVersion) {
				throw new BusinessException("版本号小于历史版本号");
			}
			if (appUpdate.getId()!=null&&currentVersion<=dbVersion&&!appUpdateList.get(0).getId().equals(appUpdate.getId())) {
				throw new BusinessException("当前版本号必须大于历史版本号");
			}
			AppUpdate selectByVersion = this.appUpdateMapper.selectByVersion(appUpdate.getVersion());
			if (selectByVersion!=null&&appUpdate.getId()!=null&&!selectByVersion.getId().equals(appUpdate.getId())) {
				throw new BusinessException("版本号已存在");
			}
		}
		if (appUpdate.getId()==null){
			appUpdate.setStatus(AppUpdateStatusEnum.INIT.getStatus());
			Date curTime = new Date();
			appUpdate.setCreateTime(curTime);
			this.appUpdateMapper.insert(appUpdate);
		}else {
			this.appUpdateMapper.updateById(appUpdate,appUpdate.getId());
		}
		if(file!=null){
			File file1 = new File(file.getOriginalFilename() + Constants.APP_UPDATE_FOLDER);
			if (!file1.exists()) {
				file1.mkdirs();
			}
			String filePath = file1.getAbsolutePath() +"/"+appUpdate.getId()+Constants.APP_EXE_SUFFIX;
			file.transferTo(new File(filePath));
		}


	}

	@Override
	public void postUpdate(Integer id, Integer status, String grayScaleUid) {
		AppUpdate appUpdate = this.appUpdateMapper.selectById(id);
		if (appUpdate == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if (AppUpdateStatusEnum.getAppUpdateStatusEnum(status).getStatus()==null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if (!AppUpdateStatusEnum.INIT.getStatus().equals(appUpdate.getStatus())) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if (appUpdate.getStatus().equals(AppUpdateStatusEnum.GRAYSCALE.getStatus())&& StringUtils.isEmpty(grayScaleUid)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if (!appUpdate.getStatus().equals(AppUpdateStatusEnum.GRAYSCALE)){
			grayScaleUid="";
		}
		AppUpdate appUpdate1 = new AppUpdate();
		appUpdate1.setStatus(status);
		appUpdate1.setGrayscaleId(grayScaleUid);
		this.appUpdateMapper.updateById(appUpdate1,id);
	}

	@Override
	public AppUpdate selectLatestUpdate(String appVersion, String uid) {
		this.appUpdateMapper.selectLatestByVersion(appVersion,uid);
		return null;
	}
}