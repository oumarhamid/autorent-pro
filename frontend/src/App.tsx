import {
  Navigate,
  Route,
  Routes,
} from 'react-router'

import {
  ChangePasswordPage,
} from './auth/ChangePasswordPage'
import {
  LoginPage,
} from './auth/LoginPage'
import {
  GuestRoute,
  PasswordChangeRoute,
  PermissionRoute,
  ProtectedRoute,
} from './auth/RouteGuards'
import {
  AppShell,
} from './layout/AppShell'
import {
  DashboardPage,
} from './pages/DashboardPage'
import {
  ForbiddenPage,
} from './pages/ForbiddenPage'
import {
  UsersPage,
} from './pages/UsersPage'

function App() {
  return (
    <Routes>
      <Route
        element={<GuestRoute />}
      >
        <Route
          path="/login"
          element={<LoginPage />}
        />
      </Route>

      <Route
        element={<PasswordChangeRoute />}
      >
        <Route
          path="/change-password"
          element={<ChangePasswordPage />}
        />
      </Route>

      <Route
        element={<ProtectedRoute />}
      >
        <Route
          path="/app"
          element={<AppShell />}
        >
          <Route
            index
            element={<DashboardPage />}
          />

          <Route
            element={
              <PermissionRoute
                permission="USER_READ"
                scope="GLOBAL"
              />
            }
          >
            <Route
              path="users"
              element={<UsersPage />}
            />
          </Route>
        </Route>

        <Route
          path="/forbidden"
          element={<ForbiddenPage />}
        />
      </Route>

      <Route
        path="/"
        element={
          <Navigate
            to="/app"
            replace
          />
        }
      />

      <Route
        path="*"
        element={
          <Navigate
            to="/app"
            replace
          />
        }
      />
    </Routes>
  )
}

export default App