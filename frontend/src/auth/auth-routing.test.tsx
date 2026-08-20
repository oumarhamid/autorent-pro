import {
  render,
  screen,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
  MemoryRouter,
} from 'react-router'
import {
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import App from '../App'
import {
  AuthContext,
  type AuthContextValue,
} from './auth-context'
import type {
  CurrentUser,
} from './auth.types'

const adminUser: CurrentUser = {
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
    {
      permission: 'ACCOUNT_CHANGE_PASSWORD',
      scope: 'SELF',
    },
  ],

  mustChangePassword: false,
}

function createAuthValue(
  overrides: Partial<AuthContextValue> = {},
): AuthContextValue {
  return {
    user: adminUser,

    status: 'authenticated',

    isAuthenticated: true,

    sessionError: null,

    login:
      vi.fn(),

    logout:
      vi.fn(),

    changePassword:
      vi.fn(),

    refreshUser:
      vi.fn(),

    ...overrides,
  }
}

function renderAt(
  path: string,
  authValue: AuthContextValue,
) {
  return render(
    <MemoryRouter
      initialEntries={[
        path,
      ]}
    >
      <AuthContext.Provider
        value={authValue}
      >
        <App />
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

describe('authentication routing', () => {
  it('redirects an unauthenticated user from a protected route to login', async () => {
    renderAt(
      '/app/users',
      createAuthValue({
        user: null,
        status: 'unauthenticated',
        isAuthenticated: false,
      }),
    )

    expect(
      await screen.findByRole(
        'heading',
        {
          name: 'Connexion',
        },
      ),
    ).toBeInTheDocument()
  })

  it('allows an authorized administrator to open the users route', async () => {
    renderAt(
      '/app/users',
      createAuthValue(),
    )

    expect(
      await screen.findByRole(
        'heading',
        {
          name: 'Utilisateurs',
        },
      ),
    ).toBeInTheDocument()
  })

  it('redirects a user without USER_READ GLOBAL to the forbidden page', async () => {
    renderAt(
      '/app/users',
      createAuthValue({
        user: {
          ...adminUser,
          permissions: [],
        },
      }),
    )

    expect(
      await screen.findByRole(
        'heading',
        {
          name:
            'Autorisation insuffisante',
        },
      ),
    ).toBeInTheDocument()
  })

  it('forces password change before accessing the application', async () => {
    renderAt(
      '/app',
      createAuthValue({
        user: {
          ...adminUser,
          mustChangePassword: true,
        },
      }),
    )

    expect(
      await screen.findByRole(
        'heading',
        {
          name:
            'Choisissez un nouveau mot de passe',
        },
      ),
    ).toBeInTheDocument()
  })

  it('redirects an authenticated user away from login', async () => {
    renderAt(
      '/login',
      createAuthValue(),
    )

    expect(
      await screen.findByRole(
        'heading',
        {
          name: 'Tableau de bord',
        },
      ),
    ).toBeInTheDocument()
  })

  it('hides the users navigation when permission is missing', async () => {
    renderAt(
      '/app',
      createAuthValue({
        user: {
          ...adminUser,
          permissions: [],
        },
      }),
    )

    expect(
      await screen.findByRole(
        'heading',
        {
          name: 'Tableau de bord',
        },
      ),
    ).toBeInTheDocument()

    expect(
      screen.queryByRole(
        'link',
        {
          name: 'Utilisateurs',
        },
      ),
    ).not.toBeInTheDocument()
  })

  it('calls the logout action from the authenticated shell', async () => {
    const userEventInstance =
      userEvent.setup()

    const logout =
      vi.fn()
        .mockResolvedValue(undefined)

    renderAt(
      '/app',
      createAuthValue({
        logout,
      }),
    )

    await userEventInstance.click(
      await screen.findByRole(
        'button',
        {
          name: 'Se déconnecter',
        },
      ),
    )

    expect(logout)
      .toHaveBeenCalledTimes(1)
  })

  it('redirects an unauthenticated user away from the password change page', async () => {
    renderAt(
      '/change-password',
      createAuthValue({
        user: null,
        status: 'unauthenticated',
        isAuthenticated: false,
      }),
    )

    expect(
      await screen.findByRole(
        'heading',
        {
          name: 'Connexion',
        },
      ),
    ).toBeInTheDocument()
  })
})