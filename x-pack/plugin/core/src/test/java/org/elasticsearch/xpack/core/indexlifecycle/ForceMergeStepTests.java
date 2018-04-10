/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.indexlifecycle;


import org.apache.lucene.util.SetOnce;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.index.Index;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.EqualsHashCodeTestUtils;
import org.elasticsearch.xpack.core.indexlifecycle.Step.StepKey;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ForceMergeStepTests extends ESTestCase {

    public ForceMergeStep createRandomInstance() {
        Step.StepKey stepKey = new StepKey(randomAlphaOfLength(10), randomAlphaOfLength(10), randomAlphaOfLength(10));
        StepKey nextStepKey = new StepKey(randomAlphaOfLength(10), randomAlphaOfLength(10), randomAlphaOfLength(10));
        int maxNumSegments = randomIntBetween(1, 10);

        return new ForceMergeStep(stepKey, nextStepKey, null, maxNumSegments);
    }

    public ForceMergeStep mutateInstance(ForceMergeStep instance) {
        StepKey key = instance.getKey();
        StepKey nextKey = instance.getNextStepKey();
        int maxNumSegments = instance.getMaxNumSegments();

        switch (between(0, 2)) {
            case 0:
                key = new StepKey(key.getPhase(), key.getAction(), key.getName() + randomAlphaOfLength(5));
                break;
            case 1:
                nextKey = new StepKey(key.getPhase(), key.getAction(), key.getName() + randomAlphaOfLength(5));
                break;
            case 2:
                maxNumSegments += 1;
                break;
            default:
                throw new AssertionError("Illegal randomisation branch");
        }

        return new ForceMergeStep(key, nextKey, null, maxNumSegments);
    }


    public void testHashcodeAndEquals() {
        EqualsHashCodeTestUtils.checkEqualsAndHashCode(createRandomInstance(),
            instance -> new ForceMergeStep(instance.getKey(), instance.getNextStepKey(),
                null, instance.getMaxNumSegments()), this::mutateInstance);
    }

    public void testPerformActionComplete() {
        Step.StepKey stepKey = new StepKey(randomAlphaOfLength(10), randomAlphaOfLength(10), randomAlphaOfLength(10));
        StepKey nextStepKey = new StepKey(randomAlphaOfLength(10), randomAlphaOfLength(10), randomAlphaOfLength(10));
        int maxNumSegments = randomIntBetween(1, 10);
        Client client = mock(Client.class);
        AdminClient adminClient = mock(AdminClient.class);
        IndicesAdminClient indicesClient = mock(IndicesAdminClient.class);
        when(client.admin()).thenReturn(adminClient);
        when(adminClient.indices()).thenReturn(indicesClient);
        ForceMergeResponse forceMergeResponse = Mockito.mock(ForceMergeResponse.class);
        Mockito.when(forceMergeResponse.getStatus()).thenReturn(RestStatus.OK);
        Mockito.doAnswer(invocationOnMock -> {
            ForceMergeRequest request = (ForceMergeRequest) invocationOnMock.getArguments()[0];
            assertThat(request.maxNumSegments(), equalTo(maxNumSegments));
            @SuppressWarnings("unchecked")
            ActionListener<ForceMergeResponse> listener = (ActionListener<ForceMergeResponse>) invocationOnMock.getArguments()[1];
            listener.onResponse(forceMergeResponse);
            return null;
        }).when(indicesClient).forceMerge(any(), any());

        ForceMergeStep step = new ForceMergeStep(stepKey, nextStepKey, client, maxNumSegments);
        Index index = new Index(randomAlphaOfLength(5), randomAlphaOfLength(5));
        SetOnce<Boolean> completed = new SetOnce<>();
        step.performAction(index, new AsyncActionStep.Listener() {
            @Override
            public void onResponse(boolean complete) {
                completed.set(complete);
            }

            @Override
            public void onFailure(Exception e) {
                throw new AssertionError("unexpected method call", e);
            }
        });
        assertThat(completed.get(), equalTo(true));
    }

    public void testPerformActionThrowsException() {
        Exception exception = new RuntimeException("error");
        Step.StepKey stepKey = new StepKey(randomAlphaOfLength(10), randomAlphaOfLength(10), randomAlphaOfLength(10));
        StepKey nextStepKey = new StepKey(randomAlphaOfLength(10), randomAlphaOfLength(10), randomAlphaOfLength(10));
        int maxNumSegments = randomIntBetween(1, 10);
        Client client = mock(Client.class);
        AdminClient adminClient = mock(AdminClient.class);
        IndicesAdminClient indicesClient = mock(IndicesAdminClient.class);
        when(client.admin()).thenReturn(adminClient);
        when(adminClient.indices()).thenReturn(indicesClient);
        ForceMergeResponse forceMergeResponse = Mockito.mock(ForceMergeResponse.class);
        Mockito.when(forceMergeResponse.getStatus()).thenReturn(RestStatus.OK);
        Mockito.doAnswer(invocationOnMock -> {
            ForceMergeRequest request = (ForceMergeRequest) invocationOnMock.getArguments()[0];
            assertThat(request.maxNumSegments(), equalTo(maxNumSegments));
            @SuppressWarnings("unchecked")
            ActionListener<ForceMergeResponse> listener = (ActionListener<ForceMergeResponse>) invocationOnMock.getArguments()[1];
            listener.onFailure(exception);
            return null;
        }).when(indicesClient).forceMerge(any(), any());

        ForceMergeStep step = new ForceMergeStep(stepKey, nextStepKey, client, maxNumSegments);
        Index index = new Index(randomAlphaOfLength(5), randomAlphaOfLength(5));
        SetOnce<Boolean> exceptionThrown = new SetOnce<>();
        step.performAction(index, new AsyncActionStep.Listener() {
            @Override
            public void onResponse(boolean complete) {
                throw new AssertionError("unexpected method call");
            }

            @Override
            public void onFailure(Exception e) {
                assertEquals(exception, e);
                exceptionThrown.set(true);
            }
        });
        assertThat(exceptionThrown.get(), equalTo(true));
    }
}
