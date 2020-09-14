package com.ocadotechnology.newrelic.alertsconfigurator.dsl.configuration.condition.terms

import com.ocadotechnology.newrelic.alertsconfigurator.configuration.condition.terms.*
import com.ocadotechnology.newrelic.alertsconfigurator.dsl.NewRelicConfigurationMarker

@NewRelicConfigurationMarker
class NrqlTermsConfigurationDsl {
    var durationTerm: NrqlDurationTerm? = null
    var operatorTerm: OperatorTerm? = null
    var priorityTerm: PriorityTerm? = null
    var thresholdTerm: Float? = null
    var timeFunctionTerm: TimeFunctionTerm? = null
}

fun nrqlTermsConfiguration(block: NrqlTermsConfigurationDsl.() -> Unit): NrqlTermsConfiguration {
    val dsl = NrqlTermsConfigurationDsl()
    dsl.block()

    return NrqlTermsConfiguration.builder()
            .durationTerm(requireNotNull(dsl.durationTerm) { "Nrql terms duration cannot be null" })
            .operatorTerm(requireNotNull(dsl.operatorTerm) { "Nrql terms operator cannot be null" })
            .priorityTerm(requireNotNull(dsl.priorityTerm) { "Nrql terms priority cannot be null" })
            .thresholdTerm(requireNotNull(dsl.thresholdTerm) { "Nrql terms threshold cannot be null" })
            .timeFunctionTerm(requireNotNull(dsl.timeFunctionTerm) { "Nrql terms time function cannot be null" })
            .build()
}

@NewRelicConfigurationMarker
class NrqlTermConfigurations {
    internal val terms: MutableList<NrqlTermsConfiguration> = mutableListOf()

    fun term(block: NrqlTermsConfigurationDsl.() -> Unit) = terms.add(com.ocadotechnology.newrelic.alertsconfigurator.dsl.configuration.condition.terms.nrqlTermsConfiguration(block))
    operator fun NrqlTermsConfiguration.unaryPlus() = terms.add(this)

    infix fun PriorityTerm.given(timeFunctionTerm: TimeFunctionTerm) : AfterGiven {
        return AfterGiven(NrqlTermsConfiguration.builder().priorityTerm(this).timeFunctionTerm(timeFunctionTerm))
    }

    infix fun AfterGiven.inLast(durationTerm: NrqlDurationTerm) : AfterInLast {
        return AfterInLast(this.builder.durationTerm(durationTerm))
    }

    infix fun AfterInLast.minutes(operatorTerm: OperatorTerm) : AfterMinutes {
        return AfterMinutes(this.builder.operatorTerm(operatorTerm))
    }

    infix fun AfterMinutes.value(thresholdTerm: Float) {
        terms.add(this.builder.thresholdTerm(thresholdTerm).build())
    }

    class AfterGiven(internal val builder: NrqlTermsConfiguration.NrqlTermsConfigurationBuilder)
    class AfterInLast(internal val builder: NrqlTermsConfiguration.NrqlTermsConfigurationBuilder)
    class AfterMinutes(internal val builder: NrqlTermsConfiguration.NrqlTermsConfigurationBuilder)
}