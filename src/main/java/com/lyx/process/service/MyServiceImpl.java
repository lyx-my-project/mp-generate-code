package com.lyx.process.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ZipUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.lyx.common.ResponseData;
import com.lyx.entity.GenerateCodePara;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service("myServiceImpl")
public class MyServiceImpl implements MyService
{
	@Override
	public ResponseEntity geneCode(GenerateCodePara para, BindingResult result)
	{
		try
		{
			ResponseData filedCheckResult = this.fieldCheck(result);
			if (!filedCheckResult.getSuccess())
			{
				return ResponseEntity
						.ok()
						.contentType(MediaType.APPLICATION_JSON) // 设置 Content-Type 头部
						.body(" {\"error\":\"" + filedCheckResult.getMsg() + "\"} ");
			}

			File allFile = FileUtil.file(this.geneCodeFile(para)); // 总目录
			File zipFile = ZipUtil.zip(allFile); // 将总目录下的文件打包，不包括总目录

			byte[] zipFileData = FileUtil.readBytes(zipFile);

			// 删除生成的文件
			FileUtil.del(allFile);
			FileUtil.del(zipFile);

			return ResponseEntity
					.ok()
					.header("Content-Disposition", "attachment;fileName=code.zip")
					.contentType(MediaType.MULTIPART_FORM_DATA) // 设置 Content-Type 头部
					.body(zipFileData);
		}
		catch (Exception e)
		{
			return ResponseEntity
					.ok()
					.contentType(MediaType.APPLICATION_JSON) // 设置 Content-Type 头部
					.body(" {\"error\":\"连接数据库失败，请检查主机以用用户名密码是否正确\"} ");
		}
	}

	/**
	 * 生成代码
	 * @param para 生成的文件的总路径，要把这个文件打包下载，然后删除
	 */
	private String geneCodeFile(GenerateCodePara para)
	{
		AutoGenerator mpg = new AutoGenerator();

		GlobalConfig gc = new GlobalConfig();
		String projectPath = FileUtil.getTmpDirPath() + "z" + UUID.randomUUID(); // 项目的路径
		gc.setOutputDir(projectPath + "/src/main/java");
		gc.setAuthor("powered by lyx"); // 作者
		gc.setSwagger2(para.getUseSwagger()); // 是否添加Swagger2注解
		mpg.setGlobalConfig(gc);


		DataSourceConfig dsc = new DataSourceConfig();
		dsc.setUrl(new StringBuilder("jdbc:mysql://").append(para.getDbHost()).append(':').append(para.getPort()).append('/').append(para.getDb()).append("?serverTimezone=Asia/Shanghai").toString()); // 数据库地址
		dsc.setDriverName("com.mysql.cj.jdbc.Driver"); // 连接数据库驱动
		dsc.setUsername(para.getUser()); // 用户名
		dsc.setPassword(para.getPasswd()); // 密码
		mpg.setDataSource(dsc);

		PackageConfig pc = new PackageConfig();
		pc.setModuleName(para.getModuleName());
		pc.setParent(para.getParentPackage()); // 设置 "父包名"
		mpg.setPackageInfo(pc);

		if (para.getGeXml()) // 是否生成xml文件
		{
			boolean inPackage = para.getWhereMapperXml(); // 是否在包内

			InjectionConfig cfg = new InjectionConfig()
			{
				@Override
				public void initMap()
				{
				}
			};


			String templatePath = "/templates/mapper.xml.ftl";

			List<FileOutConfig> focList = new ArrayList<>();
			focList.add
			(
				new FileOutConfig(templatePath)
				{
					@Override
					public String outputFile(TableInfo tableInfo)
					{
						if (inPackage)
						{
							return projectPath + "/src/main/java/" + pc.getParent().replace(".", "/") + "/mapper/" + tableInfo.getEntityName() + "Mapper" + StringPool.DOT_XML;
						}
						else
						{
							return projectPath + "/src/main/resources/mapper/" + pc.getModuleName() + "/" + tableInfo.getEntityName() + "Mapper" + StringPool.DOT_XML;
						}
					}
				}
			);

			cfg.setFileOutConfigList(focList);
			mpg.setCfg(cfg);
		}

		TemplateConfig templateConfig = new TemplateConfig();


		templateConfig.setXml(null);
		mpg.setTemplate(templateConfig);


		StrategyConfig strategy = new StrategyConfig();

		strategy.setNaming(para.getEntityName2TableName() ? NamingStrategy.no_change : NamingStrategy.underline_to_camel);
		strategy.setColumnNaming(para.getFieldName2AttrName() ? NamingStrategy.no_change : NamingStrategy.underline_to_camel);  // 字段到属性
		strategy.setEntityLombokModel(para.getAddLombok()); // 是否添加 lombok 注解
		strategy.setRestControllerStyle(para.getUseRest()); // 是否为RestController
		strategy.setEntityTableFieldAnnotationEnable(para.getEntityAddTableNameAnno()); // 生成实体时是加 @TableField 注解


		strategy.setInclude(para.getTable().split(","));
		strategy.setTablePrefix(pc.getModuleName() + "_");

		mpg.setStrategy(strategy);
		mpg.setTemplateEngine(new FreemarkerTemplateEngine());
		mpg.execute();

		return projectPath;
	}

	/**
	 * 检查字段
	 * @param bindingResult 数据绑定结果
	 */
	ResponseData fieldCheck(BindingResult bindingResult)
	{
		if (!bindingResult.hasErrors()) // 数据绑定没有出现错误
		{
			return ResponseData.success();
		}

		// 获得第一个错误，因为我们只需要输出第一个错误，后边的也没有必要输出
		FieldError error = bindingResult.getFieldErrors().get(0);

		return ResponseData.errorMsg(error.getDefaultMessage()); // 这里获得的message就是注解中写的那个
	}
}