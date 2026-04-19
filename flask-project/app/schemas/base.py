from pydantic import BaseModel, ConfigDict


class StrictModel(BaseModel):
    """Base model enforcing strict parsing and no extra fields."""

    model_config = ConfigDict(
        extra="forbid",
        str_strip_whitespace=True,
        strict=True,
        populate_by_name=True,
    )
