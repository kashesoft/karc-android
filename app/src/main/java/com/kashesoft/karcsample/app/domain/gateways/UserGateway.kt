/*
 * Copyright (C) 2018 Kashesoft
 */

package com.kashesoft.karcsample.app.domain.gateways

import com.kashesoft.karcsample.app.domain.entities.User
import io.reactivex.Observable

interface UserGateway {
    fun fetchUsers(userIds: List<Int>): Observable<User>
}