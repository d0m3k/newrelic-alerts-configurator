package com.ocado.panda.newrelic.client;

import com.ocado.pandateam.newrelic.sync.Synchronizer;
import com.ocado.pandateam.newrelic.sync.configuration.ApplicationConfiguration;
import com.ocado.pandateam.newrelic.sync.configuration.ChannelConfiguration;
import com.ocado.pandateam.newrelic.sync.configuration.ConditionConfiguration;
import com.ocado.pandateam.newrelic.sync.configuration.PolicyConfiguration;
import com.ocado.pandateam.newrelic.sync.configuration.channel.EmailChannel;
import com.ocado.pandateam.newrelic.sync.configuration.condition.ApmAppCondition;
import com.ocado.pandateam.newrelic.sync.configuration.condition.ConditionScope;
import com.ocado.pandateam.newrelic.sync.configuration.condition.terms.DurationTerm;
import com.ocado.pandateam.newrelic.sync.configuration.condition.terms.OperatorTerm;
import com.ocado.pandateam.newrelic.sync.configuration.condition.terms.PriorityTerm;
import com.ocado.pandateam.newrelic.sync.configuration.condition.terms.TermsConfiguration;
import com.ocado.pandateam.newrelic.sync.configuration.condition.terms.TimeFunctionTerm;

import java.util.Collection;
import java.util.Collections;

public class Main {
    private static final String APPLICATION_NAME = "user_management";
    private static final String POLICY_NAME = "User Management";

    public static void main(String[] args) {
        if (args.length == 0 || args[0].length() == 0) {
            throw new IllegalArgumentException("Missing API key");
        } else {
            String apiKey = args[0];

            Synchronizer synchronizer = new Synchronizer(apiKey);
            synchronizer.setApplicationConfigurations(createApplicationConfigurations());
            synchronizer.setPolicyConfigurations(createPolicyConfigurations());
            synchronizer.setConditionConfigurations(createConditionsConfigurations());
            synchronizer.setChannelConfigurations(createChannelConfigurations());

            synchronizer.sync();
        }
    }

    private static Collection<ApplicationConfiguration> createApplicationConfigurations(){
        return Collections.singleton(
            ApplicationConfiguration.builder()
                .applicationName(APPLICATION_NAME)
                .appApdexThreshold(0.5f)
                .endUserApdexThreshold(7.0f)
                .enableRealUserMonitoring(true)
                .build()
        );
    }

    private static Collection<PolicyConfiguration> createPolicyConfigurations() {
        return Collections.singleton(
            PolicyConfiguration.builder()
                .policyName(POLICY_NAME)
                .incidentPreference(PolicyConfiguration.IncidentPreference.PER_POLICY)
                .build()
        );
    }

    private static Collection<ConditionConfiguration> createConditionsConfigurations() {
        return Collections.singleton(
            ConditionConfiguration.builder()
                .policyName(POLICY_NAME)
                .conditions(
                    Collections.singletonList(
                        ApmAppCondition.builder()
                            .conditionName("Apdex score (Low)")
                            .enabled(true)
                            .entities(Collections.singletonList(APPLICATION_NAME))
                            .metric(ApmAppCondition.Metric.APDEX)
                            .conditionScope(ConditionScope.APPLICATION)
                            .terms(
                                Collections.singletonList(
                                    TermsConfiguration.builder()
                                        .durationTerm(DurationTerm.DURATION_5)
                                        .operatorTerm(OperatorTerm.BELOW)
                                        .priorityTerm(PriorityTerm.CRITICAL)
                                        .timeFunctionTerm(TimeFunctionTerm.ALL)
                                        .thresholdTerm(0.8f)
                                        .build()
                                )
                            )
                            .build()
                    )
                )
                .build()
        );
    }

    private static Collection<ChannelConfiguration> createChannelConfigurations() {
        return Collections.singleton(
            ChannelConfiguration.builder()
                .policyName(POLICY_NAME)
                .channels(
                    Collections.singletonList(
                        EmailChannel.builder()
                            .channelName("Panda team")
                            .emailAddress("core-services-xd@ocado.com")
                            .includeJsonAttachment(false)
                            .build()
                    ))
                .build()
        );
    }
}
