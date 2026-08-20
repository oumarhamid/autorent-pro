import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  hasPermission,
} from './authorization'
import type {
  CurrentUser,
} from './auth.types'

const user: CurrentUser = {
  userId:
    '10000000-0000-0000-0000-000000000001',

  email:
    'admin@autorent.local',

  roles: [
    'ADMIN',
  ],

  permissions: [
    {
      permission: 'USER_READ',
      scope: 'GLOBAL',
    },
    {
      permission: 'ACCOUNT_READ',
      scope: 'SELF',
    },
  ],

  mustChangePassword: false,
}

describe('hasPermission', () => {
  it('accepts an exact permission and scope grant', () => {
    expect(
      hasPermission(
        user,
        'USER_READ',
        'GLOBAL',
      ),
    ).toBe(true)
  })

  it('rejects the correct permission with another scope', () => {
    expect(
      hasPermission(
        user,
        'USER_READ',
        'AGENCY',
      ),
    ).toBe(false)
  })

  it('rejects a permission that was not granted', () => {
    expect(
      hasPermission(
        user,
        'USER_CREATE',
        'GLOBAL',
      ),
    ).toBe(false)
  })
})