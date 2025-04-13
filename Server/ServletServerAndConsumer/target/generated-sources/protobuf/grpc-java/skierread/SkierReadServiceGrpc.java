package skierread;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.61.0)",
    comments = "Source: SkierReadService.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class SkierReadServiceGrpc {

  private SkierReadServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "skierread.SkierReadService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<skierread.SkierReadServiceOuterClass.VerticalRequest,
      skierread.SkierReadServiceOuterClass.VerticalListResponse> getGetTotalVerticalMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetTotalVertical",
      requestType = skierread.SkierReadServiceOuterClass.VerticalRequest.class,
      responseType = skierread.SkierReadServiceOuterClass.VerticalListResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<skierread.SkierReadServiceOuterClass.VerticalRequest,
      skierread.SkierReadServiceOuterClass.VerticalListResponse> getGetTotalVerticalMethod() {
    io.grpc.MethodDescriptor<skierread.SkierReadServiceOuterClass.VerticalRequest, skierread.SkierReadServiceOuterClass.VerticalListResponse> getGetTotalVerticalMethod;
    if ((getGetTotalVerticalMethod = SkierReadServiceGrpc.getGetTotalVerticalMethod) == null) {
      synchronized (SkierReadServiceGrpc.class) {
        if ((getGetTotalVerticalMethod = SkierReadServiceGrpc.getGetTotalVerticalMethod) == null) {
          SkierReadServiceGrpc.getGetTotalVerticalMethod = getGetTotalVerticalMethod =
              io.grpc.MethodDescriptor.<skierread.SkierReadServiceOuterClass.VerticalRequest, skierread.SkierReadServiceOuterClass.VerticalListResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetTotalVertical"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  skierread.SkierReadServiceOuterClass.VerticalRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  skierread.SkierReadServiceOuterClass.VerticalListResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SkierReadServiceMethodDescriptorSupplier("GetTotalVertical"))
              .build();
        }
      }
    }
    return getGetTotalVerticalMethod;
  }

  private static volatile io.grpc.MethodDescriptor<skierread.SkierReadServiceOuterClass.SkierDayRequest,
      skierread.SkierReadServiceOuterClass.VerticalIntResponse> getGetSkierDayRidesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetSkierDayRides",
      requestType = skierread.SkierReadServiceOuterClass.SkierDayRequest.class,
      responseType = skierread.SkierReadServiceOuterClass.VerticalIntResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<skierread.SkierReadServiceOuterClass.SkierDayRequest,
      skierread.SkierReadServiceOuterClass.VerticalIntResponse> getGetSkierDayRidesMethod() {
    io.grpc.MethodDescriptor<skierread.SkierReadServiceOuterClass.SkierDayRequest, skierread.SkierReadServiceOuterClass.VerticalIntResponse> getGetSkierDayRidesMethod;
    if ((getGetSkierDayRidesMethod = SkierReadServiceGrpc.getGetSkierDayRidesMethod) == null) {
      synchronized (SkierReadServiceGrpc.class) {
        if ((getGetSkierDayRidesMethod = SkierReadServiceGrpc.getGetSkierDayRidesMethod) == null) {
          SkierReadServiceGrpc.getGetSkierDayRidesMethod = getGetSkierDayRidesMethod =
              io.grpc.MethodDescriptor.<skierread.SkierReadServiceOuterClass.SkierDayRequest, skierread.SkierReadServiceOuterClass.VerticalIntResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetSkierDayRides"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  skierread.SkierReadServiceOuterClass.SkierDayRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  skierread.SkierReadServiceOuterClass.VerticalIntResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SkierReadServiceMethodDescriptorSupplier("GetSkierDayRides"))
              .build();
        }
      }
    }
    return getGetSkierDayRidesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<skierread.SkierReadServiceOuterClass.ResortDayRequest,
      skierread.SkierReadServiceOuterClass.SkierCountResponse> getGetResortDaySkiersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetResortDaySkiers",
      requestType = skierread.SkierReadServiceOuterClass.ResortDayRequest.class,
      responseType = skierread.SkierReadServiceOuterClass.SkierCountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<skierread.SkierReadServiceOuterClass.ResortDayRequest,
      skierread.SkierReadServiceOuterClass.SkierCountResponse> getGetResortDaySkiersMethod() {
    io.grpc.MethodDescriptor<skierread.SkierReadServiceOuterClass.ResortDayRequest, skierread.SkierReadServiceOuterClass.SkierCountResponse> getGetResortDaySkiersMethod;
    if ((getGetResortDaySkiersMethod = SkierReadServiceGrpc.getGetResortDaySkiersMethod) == null) {
      synchronized (SkierReadServiceGrpc.class) {
        if ((getGetResortDaySkiersMethod = SkierReadServiceGrpc.getGetResortDaySkiersMethod) == null) {
          SkierReadServiceGrpc.getGetResortDaySkiersMethod = getGetResortDaySkiersMethod =
              io.grpc.MethodDescriptor.<skierread.SkierReadServiceOuterClass.ResortDayRequest, skierread.SkierReadServiceOuterClass.SkierCountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetResortDaySkiers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  skierread.SkierReadServiceOuterClass.ResortDayRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  skierread.SkierReadServiceOuterClass.SkierCountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SkierReadServiceMethodDescriptorSupplier("GetResortDaySkiers"))
              .build();
        }
      }
    }
    return getGetResortDaySkiersMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SkierReadServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SkierReadServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SkierReadServiceStub>() {
        @java.lang.Override
        public SkierReadServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SkierReadServiceStub(channel, callOptions);
        }
      };
    return SkierReadServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SkierReadServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SkierReadServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SkierReadServiceBlockingStub>() {
        @java.lang.Override
        public SkierReadServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SkierReadServiceBlockingStub(channel, callOptions);
        }
      };
    return SkierReadServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SkierReadServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SkierReadServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SkierReadServiceFutureStub>() {
        @java.lang.Override
        public SkierReadServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SkierReadServiceFutureStub(channel, callOptions);
        }
      };
    return SkierReadServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void getTotalVertical(skierread.SkierReadServiceOuterClass.VerticalRequest request,
        io.grpc.stub.StreamObserver<skierread.SkierReadServiceOuterClass.VerticalListResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetTotalVerticalMethod(), responseObserver);
    }

    /**
     */
    default void getSkierDayRides(skierread.SkierReadServiceOuterClass.SkierDayRequest request,
        io.grpc.stub.StreamObserver<skierread.SkierReadServiceOuterClass.VerticalIntResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSkierDayRidesMethod(), responseObserver);
    }

    /**
     */
    default void getResortDaySkiers(skierread.SkierReadServiceOuterClass.ResortDayRequest request,
        io.grpc.stub.StreamObserver<skierread.SkierReadServiceOuterClass.SkierCountResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetResortDaySkiersMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service SkierReadService.
   */
  public static abstract class SkierReadServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return SkierReadServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service SkierReadService.
   */
  public static final class SkierReadServiceStub
      extends io.grpc.stub.AbstractAsyncStub<SkierReadServiceStub> {
    private SkierReadServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SkierReadServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SkierReadServiceStub(channel, callOptions);
    }

    /**
     */
    public void getTotalVertical(skierread.SkierReadServiceOuterClass.VerticalRequest request,
        io.grpc.stub.StreamObserver<skierread.SkierReadServiceOuterClass.VerticalListResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetTotalVerticalMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getSkierDayRides(skierread.SkierReadServiceOuterClass.SkierDayRequest request,
        io.grpc.stub.StreamObserver<skierread.SkierReadServiceOuterClass.VerticalIntResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetSkierDayRidesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getResortDaySkiers(skierread.SkierReadServiceOuterClass.ResortDayRequest request,
        io.grpc.stub.StreamObserver<skierread.SkierReadServiceOuterClass.SkierCountResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetResortDaySkiersMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service SkierReadService.
   */
  public static final class SkierReadServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<SkierReadServiceBlockingStub> {
    private SkierReadServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SkierReadServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SkierReadServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public skierread.SkierReadServiceOuterClass.VerticalListResponse getTotalVertical(skierread.SkierReadServiceOuterClass.VerticalRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetTotalVerticalMethod(), getCallOptions(), request);
    }

    /**
     */
    public skierread.SkierReadServiceOuterClass.VerticalIntResponse getSkierDayRides(skierread.SkierReadServiceOuterClass.SkierDayRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetSkierDayRidesMethod(), getCallOptions(), request);
    }

    /**
     */
    public skierread.SkierReadServiceOuterClass.SkierCountResponse getResortDaySkiers(skierread.SkierReadServiceOuterClass.ResortDayRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetResortDaySkiersMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service SkierReadService.
   */
  public static final class SkierReadServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<SkierReadServiceFutureStub> {
    private SkierReadServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SkierReadServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SkierReadServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<skierread.SkierReadServiceOuterClass.VerticalListResponse> getTotalVertical(
        skierread.SkierReadServiceOuterClass.VerticalRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetTotalVerticalMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<skierread.SkierReadServiceOuterClass.VerticalIntResponse> getSkierDayRides(
        skierread.SkierReadServiceOuterClass.SkierDayRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetSkierDayRidesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<skierread.SkierReadServiceOuterClass.SkierCountResponse> getResortDaySkiers(
        skierread.SkierReadServiceOuterClass.ResortDayRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetResortDaySkiersMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_TOTAL_VERTICAL = 0;
  private static final int METHODID_GET_SKIER_DAY_RIDES = 1;
  private static final int METHODID_GET_RESORT_DAY_SKIERS = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_TOTAL_VERTICAL:
          serviceImpl.getTotalVertical((skierread.SkierReadServiceOuterClass.VerticalRequest) request,
              (io.grpc.stub.StreamObserver<skierread.SkierReadServiceOuterClass.VerticalListResponse>) responseObserver);
          break;
        case METHODID_GET_SKIER_DAY_RIDES:
          serviceImpl.getSkierDayRides((skierread.SkierReadServiceOuterClass.SkierDayRequest) request,
              (io.grpc.stub.StreamObserver<skierread.SkierReadServiceOuterClass.VerticalIntResponse>) responseObserver);
          break;
        case METHODID_GET_RESORT_DAY_SKIERS:
          serviceImpl.getResortDaySkiers((skierread.SkierReadServiceOuterClass.ResortDayRequest) request,
              (io.grpc.stub.StreamObserver<skierread.SkierReadServiceOuterClass.SkierCountResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getGetTotalVerticalMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              skierread.SkierReadServiceOuterClass.VerticalRequest,
              skierread.SkierReadServiceOuterClass.VerticalListResponse>(
                service, METHODID_GET_TOTAL_VERTICAL)))
        .addMethod(
          getGetSkierDayRidesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              skierread.SkierReadServiceOuterClass.SkierDayRequest,
              skierread.SkierReadServiceOuterClass.VerticalIntResponse>(
                service, METHODID_GET_SKIER_DAY_RIDES)))
        .addMethod(
          getGetResortDaySkiersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              skierread.SkierReadServiceOuterClass.ResortDayRequest,
              skierread.SkierReadServiceOuterClass.SkierCountResponse>(
                service, METHODID_GET_RESORT_DAY_SKIERS)))
        .build();
  }

  private static abstract class SkierReadServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SkierReadServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return skierread.SkierReadServiceOuterClass.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SkierReadService");
    }
  }

  private static final class SkierReadServiceFileDescriptorSupplier
      extends SkierReadServiceBaseDescriptorSupplier {
    SkierReadServiceFileDescriptorSupplier() {}
  }

  private static final class SkierReadServiceMethodDescriptorSupplier
      extends SkierReadServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    SkierReadServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (SkierReadServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SkierReadServiceFileDescriptorSupplier())
              .addMethod(getGetTotalVerticalMethod())
              .addMethod(getGetSkierDayRidesMethod())
              .addMethod(getGetResortDaySkiersMethod())
              .build();
        }
      }
    }
    return result;
  }
}
