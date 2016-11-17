package com.ocado.pandateam.newrelic.sync;

import com.google.common.collect.ImmutableList;
import com.ocado.pandateam.newrelic.api.model.channels.AlertChannel;
import com.ocado.pandateam.newrelic.api.model.channels.AlertChannelConfiguration;
import com.ocado.pandateam.newrelic.api.model.channels.AlertChannelLinks;
import com.ocado.pandateam.newrelic.api.model.policies.AlertsPolicy;
import com.ocado.pandateam.newrelic.api.model.policies.AlertsPolicyChannels;
import com.ocado.pandateam.newrelic.sync.configuration.ChannelConfiguration;
import com.ocado.pandateam.newrelic.sync.configuration.channel.Channel;
import com.ocado.pandateam.newrelic.sync.configuration.channel.EmailChannel;
import com.ocado.pandateam.newrelic.sync.configuration.channel.SlackChannel;
import com.ocado.pandateam.newrelic.sync.exception.NewRelicSyncException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ChannelSynchronizerTest extends AbstractSynchronizerTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private ChannelSynchronizer testee;
    private ChannelConfiguration channelConfiguration = createConfiguration();

    private static final String POLICY_NAME = "policyName";
    private static final AlertsPolicy POLICY = AlertsPolicy.builder().id(42).name(POLICY_NAME).build();

    private static final String EMAIL_CHANNEL_NAME = "emailChannel";
    private static final String SLACK_CHANNEL_NAME = "slackChannel";
    private static final Channel EMAIL_CHANNEL = EmailChannel.builder().channelName(EMAIL_CHANNEL_NAME).emailAddress("recipents").build();
    private static final Channel SLACK_CHANNEL = SlackChannel.builder().channelName(SLACK_CHANNEL_NAME).slackUrl("url").build();
    private static final AlertChannelConfiguration EMAIL_CHANNEL_CONFIG = EMAIL_CHANNEL.getAlertChannelConfiguration();
    private static final AlertChannelConfiguration SLACK_CHANNEL_CONFIG = SLACK_CHANNEL.getAlertChannelConfiguration();
    private static final AlertChannelConfiguration ALERT_CHANNEL_CONFIG = AlertChannelConfiguration.builder().build();
    private static final AlertChannel EMAIL_CHANNEL_CONFIG_MAPPED = createAlertChannel(EMAIL_CHANNEL_NAME, EMAIL_CHANNEL.getType(), EMAIL_CHANNEL.getAlertChannelConfiguration());
    private static final AlertChannel SLACK_CHANNEL_CONFIG_MAPPED = createAlertChannel(SLACK_CHANNEL_NAME, SLACK_CHANNEL.getType(), SLACK_CHANNEL.getAlertChannelConfiguration());
    private static final AlertChannel EMAIL_ALERT_CHANNEL_SAME = createAlertChannel(1, EMAIL_CHANNEL_NAME, "email", EMAIL_CHANNEL_CONFIG);
    private static final AlertChannel EMAIL_ALERT_CHANNEL_SAMEINSTANCE = createAlertChannel(2, EMAIL_CHANNEL_NAME, "email", ALERT_CHANNEL_CONFIG);
    private static final AlertChannel EMAIL_ALERT_CHANNEL_DIFFERENT = createAlertChannel(3, "different", "email", ALERT_CHANNEL_CONFIG);
    private static final AlertChannel SLACK_ALERT_CHANNEL_SAME = createAlertChannel(4, SLACK_CHANNEL_NAME, "slack", SLACK_CHANNEL_CONFIG);

    @Before
    public void setUp() {
        testee = new ChannelSynchronizer(apiMock, channelConfiguration);

        when(alertsChannelsApiMock.create(eq(EMAIL_CHANNEL_CONFIG_MAPPED))).thenReturn(EMAIL_ALERT_CHANNEL_SAME);
        when(alertsChannelsApiMock.create(eq(SLACK_CHANNEL_CONFIG_MAPPED))).thenReturn(SLACK_ALERT_CHANNEL_SAME);
        when(alertsPoliciesApiMock.getByName(eq(POLICY_NAME))).thenReturn(Optional.of(POLICY));
    }

    @Test
    public void shouldThrowException_whenPolicyDoesNotExist() throws NewRelicSyncException {
        // given
        when(alertsPoliciesApiMock.getByName(eq(POLICY_NAME))).thenReturn(Optional.empty());

        // then - exception
        expectedException.expect(NewRelicSyncException.class);

        // when
        testee.sync();
    }

    @Test
    public void shouldRemoveSameInstanceChannelsAndCreateNewOne_whenChannelUpdated() throws NewRelicSyncException {
        // given
        when(alertsChannelsApiMock.list()).thenReturn(ImmutableList.of(EMAIL_ALERT_CHANNEL_SAMEINSTANCE, EMAIL_ALERT_CHANNEL_DIFFERENT));
        // when
        testee.sync();

        // then
        verify(alertsChannelsApiMock).list();
        verify(alertsChannelsApiMock).delete(eq(EMAIL_ALERT_CHANNEL_SAMEINSTANCE.getId()));
        verify(alertsChannelsApiMock).create(eq(EMAIL_CHANNEL_CONFIG_MAPPED));
        verify(alertsChannelsApiMock).create(eq(SLACK_CHANNEL_CONFIG_MAPPED));
        verifyNoMoreInteractions(alertsChannelsApiMock);
    }

    @Test
    public void shouldNotRemoveChannel_whenChannelNotUpdated() throws NewRelicSyncException {
        // given
        when(alertsChannelsApiMock.list()).thenReturn(ImmutableList.of(EMAIL_ALERT_CHANNEL_SAME, EMAIL_ALERT_CHANNEL_DIFFERENT));
        // when
        testee.sync();

        // then
        verify(alertsChannelsApiMock).list();
        verify(alertsChannelsApiMock).create(eq(SLACK_CHANNEL_CONFIG_MAPPED));
        verifyNoMoreInteractions(alertsChannelsApiMock);
    }

    @Test
    public void shouldUpdateAlertPolicyChannels() throws NewRelicSyncException {
        // given
        AlertsPolicyChannels expected = AlertsPolicyChannels.builder()
                .policyId(POLICY.getId())
                .channelIds(ImmutableList.of(EMAIL_ALERT_CHANNEL_SAME.getId(), SLACK_ALERT_CHANNEL_SAME.getId()))
                .build();

        // when
        testee.sync();

        // then
        verify(alertsPoliciesApiMock).updateChannels(eq(expected));
    }

    private static AlertChannel createAlertChannel(String name, String type, AlertChannelConfiguration config) {
        return AlertChannel.builder().name(name).type(type).configuration(config).build();
    }

    private static AlertChannel createAlertChannel(int id, String name, String type, AlertChannelConfiguration config) {
        AlertChannelLinks alertChannelLinks = new AlertChannelLinks(Collections.emptyList());
        return AlertChannel.builder().id(id).name(name).type(type).configuration(config).links(alertChannelLinks).build();
    }

    private ChannelConfiguration createConfiguration() {
        return ChannelConfiguration.builder()
                .policyName(POLICY_NAME)
                .channels(ImmutableList.of(EMAIL_CHANNEL, SLACK_CHANNEL))
                .build();
    }
}
