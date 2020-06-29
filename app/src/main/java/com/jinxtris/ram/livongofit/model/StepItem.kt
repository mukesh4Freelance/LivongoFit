package com.jinxtris.ram.livongofit.model

data class StepItem(
    var strDate: String,
    var totalCount: Int
) {
    constructor() : this(
        strDate = "",
        totalCount = 0
    )
}