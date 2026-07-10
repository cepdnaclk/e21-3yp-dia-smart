import { useState } from "react";
import {
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Switch,
  Box,
  TextField,
  InputAdornment,
  TablePagination,
  Tooltip
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import SearchIcon from "@mui/icons-material/Search";
import type { AdminUserRecord } from "../../types/admin";

interface UserDirectorySectionProps {
  title: string;
  description: string;
  addButtonLabel: string;
  users: AdminUserRecord[];
  onToggleStatus: (userId: number, currentActive: boolean) => void;
  onAddClick: () => void;
}

const UserDirectorySection: React.FC<UserDirectorySectionProps> = ({
  title,
  description,
  addButtonLabel,
  users,
  onToggleStatus,
  onAddClick
}) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(5);

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(e.target.value);
    setPage(0); // Reset to first page on search
  };

  const handleChangePage = (_event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const filteredUsers = users.filter((user) => {
    const searchLower = searchQuery.toLowerCase();
    return (
      user.displayName.toLowerCase().includes(searchLower) ||
      user.email.toLowerCase().includes(searchLower) ||
      (user.contactNumber && user.contactNumber.toLowerCase().includes(searchLower))
    );
  });

  const paginatedUsers = filteredUsers.slice(
    page * rowsPerPage,
    page * rowsPerPage + rowsPerPage
  );

  const formatDate = (dateString?: string) => {
    if (!dateString) return "Never";
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString("en-US", {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit"
      });
    } catch {
      return "Invalid Date";
    }
  };

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 3, display: "flex", flexDirection: "column" }}>
      <CardContent sx={{ flexGrow: 1, display: "flex", flexDirection: "column", gap: 2 }}>
        
        {/* Header Title & Button */}
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: 1 }}>
          <Box>
            <Typography variant="h6" sx={{ fontWeight: "bold", color: "text.primary" }}>
              {title}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {description}
            </Typography>
          </Box>
          <Button
            variant="contained"
            color="primary"
            size="small"
            startIcon={<AddIcon />}
            onClick={onAddClick}
            sx={{ textTransform: "none", borderRadius: 2, fontWeight: "medium" }}
          >
            {addButtonLabel}
          </Button>
        </Box>

        {/* Search Bar */}
        <TextField
          size="small"
          placeholder="Search by name, email, or phone..."
          value={searchQuery}
          onChange={handleSearchChange}
          fullWidth
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" color="action" />
                </InputAdornment>
              ),
            }
          }}
          sx={{
            "& .MuiOutlinedInput-root": {
              borderRadius: 2,
            }
          }}
        />

        {/* Users Table */}
        <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 2, overflow: "hidden", border: "1px solid", borderColor: "divider" }}>
          <Table size="small" aria-label={`${title} table`}>
            <TableHead sx={{ backgroundColor: "action.hover" }}>
              <TableRow>
                <TableCell sx={{ fontWeight: "bold" }}>Name</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Contact</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Last Login</TableCell>
                <TableCell align="center" sx={{ fontWeight: "bold" }}>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {paginatedUsers.map((user) => (
                <TableRow key={user.userId} hover>
                  <TableCell>
                    <Box sx={{ display: "flex", flexDirection: "column" }}>
                      <Typography variant="body2" sx={{ fontWeight: "medium" }}>
                        {user.displayName}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {user.email}
                      </Typography>
                    </Box>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="text.primary">
                      {user.contactNumber || "—"}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="caption" color="text.secondary">
                      {formatDate(user.lastLoginAt)}
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    <Tooltip title={user.active ? "Deactivate Account" : "Activate Account"}>
                      <Switch
                        size="small"
                        checked={user.active}
                        color="success"
                        onChange={() => onToggleStatus(user.userId, user.active)}
                      />
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}

              {filteredUsers.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} align="center" sx={{ py: 4 }}>
                    <Typography variant="body2" color="text.secondary">
                      No accounts found.
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>

        {/* Table Pagination */}
        {filteredUsers.length > 0 && (
          <TablePagination
            component="div"
            count={filteredUsers.length}
            page={page}
            onPageChange={handleChangePage}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={handleChangeRowsPerPage}
            rowsPerPageOptions={[5, 10, 25]}
            sx={{ borderTop: "none" }}
          />
        )}
        
      </CardContent>
    </Card>
  );
};

export default UserDirectorySection;
