/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.domain.gateways

import com.kashesoft.karc.core.gateway.Gateway
import com.kashesoft.karcsample.app.domain.entities.User
import io.reactivex.Observable

interface AbcGateway : Gateway {
    fun fetchUsers(userIds: List<Int>): Observable<User>
}