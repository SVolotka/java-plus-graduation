package ru.practicum.analyzer.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.yandex.practicum.grpc.stats.dashboard.InteractionsCountRequestProto;
import ru.yandex.practicum.grpc.stats.dashboard.RecommendationsControllerGrpc;
import ru.yandex.practicum.grpc.stats.dashboard.RecommendedEventProto;
import ru.yandex.practicum.grpc.stats.dashboard.SimilarEventsRequestProto;
import ru.yandex.practicum.grpc.stats.dashboard.UserInteractionCheckRequest;
import ru.yandex.practicum.grpc.stats.dashboard.UserInteractionCheckResponse;
import ru.yandex.practicum.grpc.stats.dashboard.UserPredictionsRequestProto;

@GrpcService
@RequiredArgsConstructor
public class RecommendationsControllerImpl extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        recommendationService.getRecommendationsForUser(request.getUserId(), request.getMaxResults())
                .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        recommendationService.getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults())
                .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        recommendationService.getInteractionsCount(request.getEventIdList())
                .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void checkUserInteraction(UserInteractionCheckRequest request,
                                     StreamObserver<UserInteractionCheckResponse> responseObserver) {
        boolean hasInteraction = recommendationService.hasUserInteraction(request.getUserId(), request.getEventId());
        responseObserver.onNext(UserInteractionCheckResponse.newBuilder()
                .setHasInteraction(hasInteraction)
                .build());
        responseObserver.onCompleted();
    }
}