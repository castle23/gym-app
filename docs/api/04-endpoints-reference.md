# Endpoints Reference

All endpoints are accessible via the API Gateway at `http://localhost:8080`. Each service also exposes endpoints directly on its own port.

## Auth Service — `/auth`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | No | Register new user |
| POST | `/auth/login` | No | Authenticate user |
| POST | `/auth/verify` | No | Verify email |
| POST | `/auth/password/request-reset` | No | Request password reset link |
| POST | `/auth/password/reset` | No | Confirm password reset |
| POST | `/auth/professional/request` | Yes | Request professional status |
| GET | `/auth/admin/professional-requests` | Yes | Get pending professional requests |
| POST | `/auth/admin/professional-requests/{id}/approve` | Yes | Approve professional request |
| POST | `/auth/admin/professional-requests/{id}/reject` | Yes | Reject professional request |
| POST | `/auth/refresh` | Yes | Refresh access token |
| GET | `/auth/profile` | Yes | Get current user profile |

## Training Service — `/training`

### Exercises
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/training/api/v1/exercises/system` | No | List system exercises |
| GET | `/training/api/v1/exercises/search?name=&type=` | No | Search by name (partial) and/or type |
| GET | `/training/api/v1/exercises/discipline/{id}` | No | Exercises by discipline |
| GET | `/training/api/v1/exercises/my-exercises` | Yes | User's exercises |
| GET | `/training/api/v1/exercises/{id}` | Yes | Get exercise |
| POST | `/training/api/v1/exercises` | Yes | Create exercise |
| PUT | `/training/api/v1/exercises/{id}` | Yes | Update exercise |
| DELETE | `/training/api/v1/exercises/{id}` | Yes | Delete exercise |

### Routine Templates
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/training/api/v1/routine-templates/system` | No | System templates |
| GET | `/training/api/v1/routine-templates/my-templates` | Yes | User templates |
| GET | `/training/api/v1/routine-templates/{id}` | Yes | Get template |
| POST | `/training/api/v1/routine-templates` | Yes | Create template |
| PUT | `/training/api/v1/routine-templates/{id}` | Yes | Update template |
| DELETE | `/training/api/v1/routine-templates/{id}` | Yes | Delete template |

### User Routines
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/training/api/v1/user-routines` | Yes | List user routines |
| GET | `/training/api/v1/user-routines/active` | Yes | Active routine |
| GET | `/training/api/v1/user-routines/{id}` | Yes | Get routine |
| POST | `/training/api/v1/user-routines/assign` | Yes | Assign routine |
| PUT | `/training/api/v1/user-routines/{id}` | Yes | Update routine |
| PATCH | `/training/api/v1/user-routines/{id}/deactivate` | Yes | Deactivate routine |
| DELETE | `/training/api/v1/user-routines/{id}` | Yes | Delete routine |

### Exercise Sessions
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/training/api/v1/exercise-sessions/routine/{id}` | Yes | Sessions by routine |
| GET | `/training/api/v1/exercise-sessions/date/{date}` | Yes | Sessions by date |
| GET | `/training/api/v1/exercise-sessions/{id}` | Yes | Get session |
| POST | `/training/api/v1/exercise-sessions` | Yes | Log session |
| PUT | `/training/api/v1/exercise-sessions/{id}` | Yes | Update session |
| DELETE | `/training/api/v1/exercise-sessions/{id}` | Yes | Delete session |

## Tracking Service — `/tracking`

### Measurements
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/tracking/api/v1/measurements` | Yes | List measurements |
| GET | `/tracking/api/v1/measurements/{id}` | Yes | Get measurement |
| GET | `/tracking/api/v1/measurements/by-type/{id}` | Yes | By type |
| GET | `/tracking/api/v1/measurements/types/{id}` | Yes | Get type |
| POST | `/tracking/api/v1/measurements/types` | Yes | Create type |
| POST | `/tracking/api/v1/measurements` | Yes | Record measurement |
| PUT | `/tracking/api/v1/measurements/{id}` | Yes | Update measurement |
| DELETE | `/tracking/api/v1/measurements/{id}` | Yes | Delete measurement |

### Objectives
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/tracking/api/v1/objectives` | Yes | List user objectives |
| GET | `/tracking/api/v1/objectives/{id}` | Yes | Get objective |
| POST | `/tracking/api/v1/objectives` | Yes | Create objective |
| PUT | `/tracking/api/v1/objectives/{id}` | Yes | Update objective |
| DELETE | `/tracking/api/v1/objectives/{id}` | Yes | Delete objective |

### Plans
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/tracking/api/v1/plans` | Yes | List user plans |
| GET | `/tracking/api/v1/plans/{id}` | Yes | Get plan |
| POST | `/tracking/api/v1/plans` | Yes | Create plan |
| PUT | `/tracking/api/v1/plans/{id}` | Yes | Update plan |
| DELETE | `/tracking/api/v1/plans/{id}` | Yes | Delete plan |

### Diet Logs
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/tracking/api/v1/diet-logs` | Yes | List user diet logs |
| GET | `/tracking/api/v1/diet-logs/{id}` | Yes | Get diet log |
| GET | `/tracking/api/v1/diet-logs/date/{date}` | Yes | By date |
| POST | `/tracking/api/v1/diet-logs` | Yes | Create diet log |
| PUT | `/tracking/api/v1/diet-logs/{id}` | Yes | Update diet log |
| DELETE | `/tracking/api/v1/diet-logs/{id}` | Yes | Delete diet log |

### Diet & Training Components
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/tracking/api/v1/diet-components/{id}` | Yes | Get diet component |
| GET | `/tracking/api/v1/plans/{planId}/diet-component` | Yes | Plan diet component |
| POST | `/tracking/api/v1/diet-components` | Yes | Create diet component |
| PUT | `/tracking/api/v1/diet-components/{id}` | Yes | Update diet component |
| DELETE | `/tracking/api/v1/diet-components/{id}` | Yes | Delete diet component |
| GET | `/tracking/api/v1/training-components/{id}` | Yes | Get training component |
| GET | `/tracking/api/v1/plans/{planId}/training-component` | Yes | Plan training component |
| POST | `/tracking/api/v1/training-components` | Yes | Create training component |
| PUT | `/tracking/api/v1/training-components/{id}` | Yes | Update training component |
| DELETE | `/tracking/api/v1/training-components/{id}` | Yes | Delete training component |

### Recommendations
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/tracking/api/v1/recommendations/{id}` | Yes | Get recommendation |
| GET | `/tracking/api/v1/recommendations/training-component/{id}` | Yes | By training component |
| GET | `/tracking/api/v1/recommendations/diet-component/{id}` | Yes | By diet component |
| POST | `/tracking/api/v1/recommendations` | Yes | Create recommendation |
| PUT | `/tracking/api/v1/recommendations/{id}` | Yes | Update recommendation |
| DELETE | `/tracking/api/v1/recommendations/{id}` | Yes | Delete recommendation |

## Notification Service — `/notifications`

### Notifications
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/notifications/api/v1/notifications` | Yes | List user notifications |
| GET | `/notifications/api/v1/notifications/unread` | Yes | Unread notifications |
| GET | `/notifications/api/v1/notifications/unread/count` | Yes | Unread count |
| POST | `/notifications/api/v1/notifications` | Yes | Create notification |
| PUT | `/notifications/api/v1/notifications/{id}/read` | Yes | Mark as read |
| DELETE | `/notifications/api/v1/notifications/{id}` | Yes | Delete notification |

### Push Tokens
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/notifications/api/v1/push-tokens` | Yes | Register push token |
| GET | `/notifications/api/v1/push-tokens` | Yes | List push tokens |
| GET | `/notifications/api/v1/push-tokens/active` | Yes | Active push tokens |
| DELETE | `/notifications/api/v1/push-tokens` | Yes | Remove push token |
