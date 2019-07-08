/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karcsample.app.domain.gateways

import com.kashesoft.karc.core.gateway.Gateway
import com.kashesoft.karcsample.app.domain.entities.User
import io.reactivex.Completable
import io.reactivex.Observable

interface AbcGateway : Gateway {
    fun fetchUsers(userIds: List<Int>): Observable<User>
    fun fetchUsers2(userIds: List<Int>): Observable<User>
    fun badRecursion(): Completable
}