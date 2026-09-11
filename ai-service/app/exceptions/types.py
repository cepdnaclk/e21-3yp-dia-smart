class AiBaseException(Exception):
    """Base exception for all controlled Dia-Smart AI service errors."""

    def __init__(self, message: str, error_code: str, status_code: int = 400):
        super().__init__(message)
        self.message = message
        self.error_code = error_code
        self.status_code = status_code


class AiUnauthorizedError(AiBaseException):
    def __init__(self, message: str = "Unauthorized access"):
        super().__init__(message, "AI_UNAUTHORIZED", status_code=401)


class AiRequestValidationError(AiBaseException):
    def __init__(self, message: str):
        super().__init__(message, "AI_REQUEST_VALIDATION_ERROR", status_code=422)


class AiUnsupportedRequestTypeError(AiBaseException):
    def __init__(self, message: str):
        super().__init__(message, "AI_UNSUPPORTED_REQUEST_TYPE", status_code=400)


class AiUnsupportedPromptVersionError(AiBaseException):
    def __init__(self, message: str):
        super().__init__(message, "AI_UNSUPPORTED_PROMPT_VERSION", status_code=400)


class AiUnsupportedProviderError(AiBaseException):
    def __init__(self, message: str):
        super().__init__(message, "AI_UNSUPPORTED_PROVIDER", status_code=400)


class AiProviderError(AiBaseException):
    def __init__(self, message: str):
        super().__init__(message, "AI_PROVIDER_ERROR", status_code=502)


class AiResponseValidationError(AiBaseException):
    def __init__(self, message: str):
        super().__init__(message, "AI_RESPONSE_VALIDATION_ERROR", status_code=502)


class AiMedicalSafetyRejectionError(AiBaseException):
    def __init__(self, message: str):
        super().__init__(message, "AI_MEDICAL_SAFETY_REJECTION", status_code=502)


class AiEvidenceValidationError(AiBaseException):
    def __init__(self, message: str):
        super().__init__(message, "AI_EVIDENCE_VALIDATION_ERROR", status_code=502)


class AiConfigurationError(AiBaseException):
    def __init__(self, message: str):
        super().__init__(message, "AI_CONFIGURATION_ERROR", status_code=500)


class AiInternalError(AiBaseException):
    def __init__(self, message: str = "Internal server error"):
        super().__init__(message, "AI_INTERNAL_ERROR", status_code=500)
