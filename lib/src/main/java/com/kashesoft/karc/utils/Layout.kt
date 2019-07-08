/*
 * Copyright (C) 2019 Kashesoft
 */

package com.kashesoft.karc.utils

//const val LAYOUT_NOT_DEFINED = -1

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Layout(val res: Int)
