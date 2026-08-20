import type {
  CurrentUser,
  PermissionCode,
  PermissionScope,
} from './auth.types'

export function hasPermission(
  user: CurrentUser,
  permission: PermissionCode,
  scope: PermissionScope,
): boolean {
  return user.permissions.some(
    (grant) =>
      grant.permission === permission
      && grant.scope === scope,
  )
}