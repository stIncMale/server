package stincmale.server.example;

import com.google.common.collect.ImmutableSet;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.ResourceLeakDetector;
import java.net.InetSocketAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import stincmale.server.Server;
import stincmale.server.netty4.MetadataMap;
import stincmale.server.netty4.NettyServer;
import stincmale.server.netty4.RequestMetadataDecoder;
import stincmale.server.netty4.tcp.http.ClientAddressMdcHandler;
import stincmale.server.netty4.tcp.http.HttpDispatchMonoHandler;
import stincmale.server.reqres.spring.http.SimpleSpringHttpRequestDispatcherByUrl;
import static io.netty.util.ResourceLeakDetector.Level.PARANOID;

@Configuration
@ComponentScan(basePackages = {"stincmale.server.example"})
@PropertySource("classpath:stincmale/server/example/exampleSpringHttpServer.properties")
class TestExampleSpringHttpServer_SpringConfig {
  @Bean
  static PropertySourcesPlaceholderConfigurer providePropertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  TestExampleSpringHttpServer_SpringConfig() {
    ResourceLeakDetector.setLevel(PARANOID);
  }

  @Bean
  ClientAddressMdcHandler provideMdcHandler() {
    return new ClientAddressMdcHandler();
  }

  @Bean
  RequestMetadataDecoder<FullHttpRequest> provideRequestMetadataDecoder() {
    return new RequestMetadataDecoder<>() {
      @Override
      protected MetadataMap createMetadata(final ChannelHandlerContext ctx, final FullHttpRequest request) {
        //add any data to requestWithMetadata, e.g. remote address from ctx
        return new MetadataMap();
      }
    };
  }

  @Bean
  HttpDispatchMonoHandler provideHttpDispatchHandler(
      final ApplicationContext appCtx,
      @Value("${prop.contextPath}") final String contextPath,
      @Value("${prop.connectionIdleTimeoutMillis}") final long connectionIdleTimeoutMillis) {
    return new HttpDispatchMonoHandler(
        new SimpleSpringHttpRequestDispatcherByUrl(
            appCtx, ImmutableSet.of("stincmale.server.example"), true, null, contextPath),
        connectionIdleTimeoutMillis);
  }

  @Bean(initMethod = "start", destroyMethod = "stop")
  Server provideHttpServer(
      final RequestMetadataDecoder<FullHttpRequest> requestMetadataDecoder,
      final ClientAddressMdcHandler clientAddressMdcHandler,
      final HttpDispatchMonoHandler httpDispatchHandler,
      @Value("${prop.bossThreads}") final int bossThreads,
      @Value("${prop.workerThreads}") final int workerThreads,
      @Value("${prop.port}") final int port) {
    final ServerBootstrap httpServerBootstrap = NettyServer.newDefaultSBootstrap()
        .group(new NioEventLoopGroup(bossThreads), new NioEventLoopGroup(workerThreads))
        .channel(NioServerSocketChannel.class)
        .localAddress(new InetSocketAddress("localhost", port))
        .childHandler(new ChannelInitializer<>() {
          @Override
          protected final void initChannel(final Channel channel) throws Exception {
            channel.pipeline()
                .addLast(new HttpServerCodec())
                .addLast(new HttpObjectAggregator(1_000_000))
                .addLast(clientAddressMdcHandler)//optional
                .addLast(requestMetadataDecoder)//optional
                .addLast(httpDispatchHandler);
          }
        });
    return new NettyServer(httpServerBootstrap);
  }
}
