This module contains a rule action class implementation to validate the context header to ensure it has to match a specific value. Otherwise, an error message should be returned to the caller.

In the RequestTransformerInterceptor, we have the following code to get the validationError from the rule engine to respond to the caller.

```
                                    case "validationError":
                                        // If the rule engine returns any validationError entry, stop the chain and send the res.
                                        // this can be either XML or JSON or TEXT. Just make sure it matches the content type
                                        String errorMessage = (String)result.get("errorMessage");
                                        String contentType = (String)result.get("contentType");
                                        int statusCode = (Integer)result.get("statusCode");
                                        if(logger.isTraceEnabled()) logger.trace("Entry key validationError with errorMessage {} contentType {} statusCode {}");
                                        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, contentType);
                                        exchange.setStatusCode(statusCode);
                                        exchange.getResponseSender().send(errorMessage);
                                        break;

```
