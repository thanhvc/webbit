0.1.13 (2011-05-09)
==================

[Full changelog](https://github.com/joewalnes/webbit/compare/v0.1.12...v0.1.13)

* Allow WebSocket message handler to throw anything. (Aslak Hellesøy)

0.1.12 (2011-05-05)
==================

[Full changelog](https://github.com/joewalnes/webbit/compare/v0.1.11...v0.1.12)

* Shut down ExecutorServices used by Netty web server (Matt Hellige)
* Fixed NPE when getting query parameters without a value: ?nothing=&some=thing (Aslak Hellesøy)
* Added EventSourceConnection.send(EventSourceMessage) (Aslak Hellesøy)
* Added BasicAuthenticationHandler for HTTP BASIC authentication (#8 Joe Walnes)

0.1.11 (2011-03-30)
==================

* Fixed a bug where static (embedded) resources were served improperly if they were of a certain size (Aslak Hellesøy)

0.1.10 (2011-03-30)
==================

* Added support for [Server-Sent Events/EventSource](http://dev.w3.org/html5/eventsource/) (#18 Aslak Hellesøy)
* Requests with full url as request uri (and not only path) are correctly matched. (Aslak Hellesøy)
* Added HttpRequest.queryParam(String key) and HttpRequest.queryParams(String key) (#20 Aslak Hellesøy)
* Added new AliasHandler for forwarding requests. (Aslak Hellesøy)
* Made it possible to call NettyHttpResponse.content() more than once. (Aslak Hellesøy)
* Added support for cookies. (#19 Aslak Hellesøy)
* Added HttpRequest.body() method. (Aslak Hellesøy)

0.1.1 (2011-02-19)
==================

* First release
