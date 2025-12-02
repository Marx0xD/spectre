from datetime import datetime
import httpx
import logging

from app.schema.instance import JobUpdateIn, Job, CallbackResult, ProblemDetail


class CallBackHandler:
    logger = logging.getLogger(__name__)

    async def make_request(self, payload:JobUpdateIn, url:str,idempotency_key:str,token:str) -> CallbackResult:
        async with httpx.AsyncClient() as client:
            try:
                headers = {
                    "X-Callback-Token": token,
                    "X-Idempotency-Key": idempotency_key
                }

                result = await client.post(url, json=payload, headers=headers)
                raw = result.json()
                self.logger.info(raw)

                # Successful path (job returned)
                if 200 <= result.status_code < 300:
                    try:
                        job = Job(**raw)
                        return CallbackResult(
                            success=True,
                            job=job,
                            error=None,
                            http_status=result.status_code,
                            raw_body=raw
                        )
                    except Exception:
                        # If Spring returns non-job JSON with 2xx
                        error = ProblemDetail(
                            title="Invalid success response",
                            detail="Spring returned unexpected structure",
                            status=result.status_code,
                            code="INVALID_RESPONSE"
                        )
                        return CallbackResult(
                            success=False,
                            job=None,
                            error=error,
                            http_status=result.status_code,
                            raw_body=raw
                        )
                elif result.status_code == 500:
                    job = Job(**raw)
                    return CallbackResult(
                        success=True,
                        job=job,
                        error=None,
                        http_status=result.status_code,
                        raw_body=raw
                    )

                # Error path (ProblemDetail returned)
                error = ProblemDetail(**raw)
                return CallbackResult(
                    success=False,
                    job=None,
                    error=error,
                    http_status=result.status_code,
                    raw_body=raw
                )



            except httpx.TimeoutException:
                return CallbackResult(
                    success=False,
                    job=None,
                    error=ProblemDetail(
                        title="Callback Timeout",
                        detail="The callback request timed out.",
                        status=408,
                        code="TIMEOUT"
                    ),
                    http_status=408,
                    raw_body={}
                )

            except httpx.ConnectError:
                return CallbackResult(
                    success=False,
                    job=None,
                    error=ProblemDetail(
                        title="Connection Error",
                        detail="Could not connect to callback URL.",
                        status=503,
                        code="CONNECT_ERROR"
                    ),
                    http_status=503,
                    raw_body={}
                )

            except httpx.ReadError:
                return CallbackResult(
                    success=False,
                    job=None,
                    error=ProblemDetail(
                        title="Read Error",
                        detail="Connection dropped while reading response.",
                        status=502,
                        code="READ_ERROR"
                    ),
                    http_status=502,
                    raw_body={}
                )

            except httpx.ProtocolError:
                return CallbackResult(
                    success=False,
                    job=None,
                    error=ProblemDetail(
                        title="Protocol Error",
                        detail="Received malformed HTTP response.",
                        status=502,
                        code="PROTOCOL_ERROR"
                    ),
                    http_status=502,
                    raw_body={}
                )

            except Exception as e:
                self.logger.error(f"Unexpected callback error: {e}")
                return CallbackResult(
                    success=False,
                    job=None,
                    error=ProblemDetail(
                        title="Unknown Error",
                        detail=str(e),
                        status=500,
                        code="UNKNOWN"
                    ),
                    http_status=500,
                    raw_body={}
                )
