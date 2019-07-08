/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.core.gateway

import com.kashesoft.karc.core.Component
import com.kashesoft.karc.core.router.Routable

abstract class GatewayImpl : Component, Gateway, Routable {

    override val logging = true
    override val loggingLifecycle = false

}
