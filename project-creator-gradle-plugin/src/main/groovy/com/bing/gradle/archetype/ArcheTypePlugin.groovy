package com.bing.gradle.archetype

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by Administrator on 2017/9/27.
 */
class ArcheTypePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.task("initProject", type: ProjectInitTask)
    }
}
