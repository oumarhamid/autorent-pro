export type RoleCode =
  | 'CLIENT'
  | 'AGENCY_AGENT'
  | 'AGENCY_MANAGER'
  | 'FLEET_MANAGER'
  | 'MANAGER'
  | 'ADMIN'

export type PermissionCode =
  | 'ACCOUNT_READ'
  | 'ACCOUNT_CHANGE_PASSWORD'
  | 'USER_READ'
  | 'USER_CREATE'
  | 'USER_UPDATE'
  | 'USER_ENABLE'
  | 'USER_DISABLE'
  | 'USER_ROLE_ASSIGN'
  | 'USER_AGENCY_ASSIGN'

export type PermissionScope =
  | 'GLOBAL'
  | 'AGENCY'
  | 'SELF'

export interface PermissionGrant {
  permission: PermissionCode
  scope: PermissionScope
}

export interface CurrentUser {
  userId: string
  email: string
  roles: RoleCode[]
  permissions: PermissionGrant[]
  mustChangePassword: boolean
}

export interface LoginRequest {
  email: string
  password: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}