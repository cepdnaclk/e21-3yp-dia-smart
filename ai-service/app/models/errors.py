from uuid import UUID

from pydantic import BaseModel, ConfigDict


class ErrorDetailResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    error_code: str
    message: str
    request_id: UUID | None = None
