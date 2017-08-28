/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.detect.bomtool.cocoapods

import com.fasterxml.jackson.annotation.JsonAnySetter

import groovy.transform.ToString

@ToString(includePackage=false, includeFields=true)
class Pod {
    String name
    String cleanName
    List<String> dependencies = []

    public Pod() {
    }

    public Pod(String name) {
        this.name = name
    }

    @JsonAnySetter
    public void setDynamicProperty(String name, List<String> dependencies) {
        this.name = name
        this.dependencies = dependencies
    }
}
