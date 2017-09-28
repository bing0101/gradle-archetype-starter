package com.bing.init.gradle

import groovy.io.FileType
import groovy.text.GStringTemplateEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
/**
 * Created by Administrator on 2017/9/27.
 */
class InitProcess {
    static final Logger LOGGER = LoggerFactory.getLogger(InitProcess.class)
    static engine = new GStringTemplateEngine()
    /**
     * zip模板文件临时解压目录
     */
    static String tempTemplateDir = "/tmp/template"
    /**
     * 文件生成目标目录
     */
    static String targetDir = "."

    static void process() {
        deleteDir(new File("."))

        String group = System.getProperty("group")
        String name = System.getProperty("name")
        String version = System.getProperty("version")
        String templatePath = System.getProperty("templatePath")
        String inputTempTemplateDir = System.getProperty("tempTemplateDir")
        String inputTargetDir = System.getProperty("targetDir")

        if (group == null || group.length() == 0) {
            println "请输入参数group"
            return
        }
        if (name == null || name.length() == 0) {
            println "请输入参数name"
            return
        }
        if (version == null || version.length() == 0) {
            version = "1.0-SNAPSHOT"
        }
        if (templatePath == null || templatePath.length() == 0) {
            println "请输入参数templatePath"
            return
        }
        if (inputTempTemplateDir != null && inputTempTemplateDir.length() > 0) {
            tempTemplateDir = inputTempTemplateDir
        }
        if (inputTargetDir != null && inputTargetDir.length() > 0) {
            targetDir = inputTargetDir
        }
        println "模板文件：${templatePath}"
        println "模板文件临时解压目录：${tempTemplateDir}"
        println "文件生成目标目录：${targetDir}"

        Map binding = [
            "group"         :group,
            "groupPath"     :group.replaceAll("\\.", "\\\\"),
            "name"          :name,
            "version"       :version,
            "projectName"   :name
        ]

        println "binding : " + binding

        List<File> templates = getTemplates(templatePath)
        templates.each { source ->
            File target = getTargetFile(Paths.get(tempTemplateDir), new File(targetDir), source, binding)
            if (source.isDirectory()) {
                return
            }
            target.parentFile.mkdirs()
            generateFromTemplate(source, target, binding)
        }
    }

    /**
     * 读取template
     * @param templatePath  template zip文件全路径，如：F:\github\study\gradle-archetype-templates-1\templates\dubbo.zip
     * @return  返回模板文件内容文件列表，包括file 和 每一级dir
     */
    private static List<File> getTemplates(String templatePath) {
        // zip 文件临时解压路径
        File templateDir = new File(tempTemplateDir)
        // 删除上次解压的临时模板文件
        deleteDir(templateDir)
        // 解压模板文件至临时文件夹
        UnZipUtils.decompress(templatePath, tempTemplateDir)

        LOGGER.info('Using template in: {}', templateDir.path)

        // 将zip模板文件解压的所有子文件放入sourceFile list，包括file 和 每一级dir
        List<File> sourceFiles = []
        templateDir.eachFileRecurse(FileType.ANY) { file ->
            sourceFiles << file
        }

        sourceFiles
    }

    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list()
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
        }
        return dir.delete()
    }

    /**
     * 解析模板子文件
     * @param sourceDirPath 模板文件临时解压目录
     * @param targetDir     模板文件解析为目标文件后的生成目录
     * @param source        带解析的目标模板子文件
     * @param binding       占位符与期望值映射
     * @return
     */
    private static File getTargetFile(Path sourceDirPath, File targetDir, File source, Map binding) {
        Path sourcePath = sourceDirPath.relativize(source.toPath())
        String rawTargetPath = new File(targetDir, resolvePaths(sourcePath)).path
        rawTargetPath = rawTargetPath.replaceAll("\\\\", "/")
        String resolvedTargetPath = engine.createTemplate(rawTargetPath).make(binding)
        new File(resolvedTargetPath)
    }

    /**
     * 解析文件路径中的占位符，由__占位符__解析为${占位符}，以待GStringTemplateEngine 利用占位符映射map，替换占位符
     * @param path
     * @return
     */
    private static String resolvePaths(Path path) {
        if (!path.toString().contains('__')) {
            path.toString()
        }
        path.collect {
            resolvePath(it.toString())
        }.join(File.separator)
    }

    /**
     * 将__占位符__解析为${占位符}
     * @param path
     * @return
     */
    private static String resolvePath(String path) {
        path.replaceAll('(.*)__([^{}/\\\\@\\n,]+)__(.*)', '$1\\$\\{$2\\}$3')
    }

    /**
     * 解析模板子文件内容
     * @param source    将要解析的模板子文件
     * @param target    将要由source生成的目标文件
     * @param binding   占位符映射map
     */
    private static void generateFromTemplate(File source, File target, Map binding) {
        try {
            target.delete()
            target << resolve(source.text, binding)
        } catch (Exception e) {
            LOGGER.error("Failed to resolve variables in: '{}]", source.path)
            LOGGER.error(e.getMessage())
            Files.copy(source.toPath(), target.toPath())
        }
    }

    /**
     * 将模板子文件内容里的@占位符@，解析为${占位符}，以待GStringTemplateEngine 利用占位符映射map，替换占位符
     * @param text      带解析的模板子文件内容，模板子文件内容占位符格式：@占位符@
     * @param binding   占位符映射map
     * @return
     */
    private static String resolve(String text, Map binding) {
        String escaped = escape(text)
        String ready = escaped.replaceAll('@([^{}/\\\\@\\n,\\s]+)@', '\\$\\{$1\\}')
        String resolved = engine.createTemplate(ready).make(binding)
        unescape(resolved)
    }

    /**
     * 转义模板子文件内容里的'$', '@'这两个字符
     * @param text
     * @return
     */
    private static String escape(String text) {
        String escaped = text.replaceAll('\\$', '__DOLLAR__')
        escaped = escaped.replaceAll('@@', '__AT__')
        escaped
    }

    /**
     * 恢复原文件内容里的'$', '@'这两个字符
     * @param resolved
     * @return
     */
    private static String unescape(String resolved) {
        String unescaped = resolved.replaceAll('__DOLLAR__', '\\$')
        unescaped = unescaped.replaceAll('__AT__', '@')
        unescaped
    }

    static void main(String[] args) {
        System.setProperty("group", "com.bing")
        System.setProperty("name", "dubbo")
        System.setProperty("templatePath", "F:\\github\\study\\gradle-archetype-templates-1\\templates\\dubbo.zip")
        process()
    }
}
