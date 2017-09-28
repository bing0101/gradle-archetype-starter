package com.bing.gradle.archetype

import com.bing.gradle.archetype.util.InitProcess
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by Administrator on 2017/9/27.
 */
class ProjectInitTask extends DefaultTask {
    @TaskAction
    init() {
        println "init project..."
        InitProcess.process()
        println "done."
    }
}
