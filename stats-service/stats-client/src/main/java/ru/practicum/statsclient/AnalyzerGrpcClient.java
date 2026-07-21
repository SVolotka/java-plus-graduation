package ru.practicum.statsclient;

import io.grpc.Deadline;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.stats.dashboard.InteractionsCountRequestProto;
import ru.yandex.practicum.grpc.stats.dashboard.RecommendationsControllerGrpc;
import ru.yandex.practicum.grpc.stats.dashboard.RecommendedEventProto;
import ru.yandex.practicum.grpc.stats.dashboard.SimilarEventsRequestProto;
import ru.yandex.practicum.grpc.stats.dashboard.UserInteractionCheckRequest;
import ru.yandex.practicum.grpc.stats.dashboard.UserPredictionsRequestProto;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class AnalyzerGrpcClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        Iterator<RecommendedEventProto> iterator = analyzerStub
                .withDeadline(Deadline.after(4, TimeUnit.SECONDS))
                .getRecommendationsForUser(request);
        return toStream(iterator);
    }

    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        Iterator<RecommendedEventProto> iterator = analyzerStub
                .withDeadline(Deadline.after(4, TimeUnit.SECONDS))
                .getSimilarEvents(request);
        return toStream(iterator);
    }

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();
        Iterator<RecommendedEventProto> iterator = analyzerStub
                .withDeadline(Deadline.after(4, TimeUnit.SECONDS))
                .getInteractionsCount(request);
        return toStream(iterator);
    }

    public boolean hasUserInteraction(long userId, long eventId) {
        UserInteractionCheckRequest request = UserInteractionCheckRequest.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .build();
        return analyzerStub
                .withDeadline(Deadline.after(4, TimeUnit.SECONDS))
                .checkUserInteraction(request)
                .getHasInteraction();
    }

    private Stream<RecommendedEventProto> toStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }
}