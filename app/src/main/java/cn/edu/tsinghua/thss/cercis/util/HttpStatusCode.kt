package cn.edu.tsinghua.thss.cercis.util

object HttpStatusCode {
    const val StatusContinue                      = 100 // RFC 7231, 6.2.1
    const val StatusSwitchingProtocols            = 101 // RFC 7231, 6.2.2
    const val StatusProcessing                    = 102 // RFC 2518, 10.1
    const val StatusEarlyHints                    = 103 // RFC 8297
    const val StatusOK                            = 200 // RFC 7231, 6.3.1
    const val StatusCreated                       = 201 // RFC 7231, 6.3.2
    const val StatusAccepted                      = 202 // RFC 7231, 6.3.3
    const val StatusNonAuthoritativeInformation   = 203 // RFC 7231, 6.3.4
    const val StatusNoContent                     = 204 // RFC 7231, 6.3.5
    const val StatusResetContent                  = 205 // RFC 7231, 6.3.6
    const val StatusPartialContent                = 206 // RFC 7233, 4.1
    const val StatusMultiStatus                   = 207 // RFC 4918, 11.1
    const val StatusAlreadyReported               = 208 // RFC 5842, 7.1
    const val StatusIMUsed                        = 226 // RFC 3229, 10.4.1
    const val StatusMultipleChoices               = 300 // RFC 7231, 6.4.1
    const val StatusMovedPermanently              = 301 // RFC 7231, 6.4.2
    const val StatusFound                         = 302 // RFC 7231, 6.4.3
    const val StatusSeeOther                      = 303 // RFC 7231, 6.4.4
    const val StatusNotModified                   = 304 // RFC 7232, 4.1
    const val StatusUseProxy                      = 305 // RFC 7231, 6.4.5
    const val StatusTemporaryRedirect             = 307 // RFC 7231, 6.4.7
    const val StatusPermanentRedirect             = 308 // RFC 7538, 3
    const val StatusBadRequest                    = 400 // RFC 7231, 6.5.1
    const val StatusUnauthorized                  = 401 // RFC 7235, 3.1
    const val StatusPaymentRequired               = 402 // RFC 7231, 6.5.2
    const val StatusForbidden                     = 403 // RFC 7231, 6.5.3
    const val StatusNotFound                      = 404 // RFC 7231, 6.5.4
    const val StatusMethodNotAllowed              = 405 // RFC 7231, 6.5.5
    const val StatusNotAcceptable                 = 406 // RFC 7231, 6.5.6
    const val StatusProxyAuthRequired             = 407 // RFC 7235, 3.2
    const val StatusRequestTimeout                = 408 // RFC 7231, 6.5.7
    const val StatusConflict                      = 409 // RFC 7231, 6.5.8
    const val StatusGone                          = 410 // RFC 7231, 6.5.9
    const val StatusLengthRequired                = 411 // RFC 7231, 6.5.10
    const val StatusPreconditionFailed            = 412 // RFC 7232, 4.2
    const val StatusRequestEntityTooLarge         = 413 // RFC 7231, 6.5.11
    const val StatusRequestURITooLong             = 414 // RFC 7231, 6.5.12
    const val StatusUnsupportedMediaType          = 415 // RFC 7231, 6.5.13
    const val StatusRequestedRangeNotSatisfiable  = 416 // RFC 7233, 4.4
    const val StatusExpectationFailed             = 417 // RFC 7231, 6.5.14
    const val StatusTeapot                        = 418 // RFC 7168, 2.3.3
    const val StatusMisdirectedRequest            = 421 // RFC 7540, 9.1.2
    const val StatusUnprocessableEntity           = 422 // RFC 4918, 11.2
    const val StatusLocked                        = 423 // RFC 4918, 11.3
    const val StatusFailedDependency              = 424 // RFC 4918, 11.4
    const val StatusTooEarly                      = 425 // RFC 8470, 5.2.
    const val StatusUpgradeRequired               = 426 // RFC 7231, 6.5.15
    const val StatusPreconditionRequired          = 428 // RFC 6585, 3
    const val StatusTooManyRequests               = 429 // RFC 6585, 4
    const val StatusRequestHeaderFieldsTooLarge   = 431 // RFC 6585, 5
    const val StatusUnavailableForLegalReasons    = 451 // RFC 7725, 3
    const val StatusInternalServerError           = 500 // RFC 7231, 6.6.1
    const val StatusNotImplemented                = 501 // RFC 7231, 6.6.2
    const val StatusBadGateway                    = 502 // RFC 7231, 6.6.3
    const val StatusServiceUnavailable            = 503 // RFC 7231, 6.6.4
    const val StatusGatewayTimeout                = 504 // RFC 7231, 6.6.5
    const val StatusHTTPVersionNotSupported       = 505 // RFC 7231, 6.6.6
    const val StatusVariantAlsoNegotiates         = 506 // RFC 2295, 8.1
    const val StatusInsufficientStorage           = 507 // RFC 4918, 11.5
    const val StatusLoopDetected                  = 508 // RFC 5842, 7.2
    const val StatusNotExtended                   = 510 // RFC 2774, 7
    const val StatusNetworkAuthenticationRequired = 511 // RFC 6585, 6
}
