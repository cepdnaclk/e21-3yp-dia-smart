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
  Tooltip,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import SearchIcon from "@mui/icons-material/Search";
import PeopleAltOutlinedIcon from "@mui/icons-material/PeopleAltOutlined";
import PersonSearchOutlinedIcon from "@mui/icons-material/PersonSearchOutlined";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";
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
  const [statusFilter, setStatusFilter] = useState("ALL");
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

  const filteredUsers = users
    .filter((user) => {
      const searchLower = searchQuery.toLowerCase();
      const matchesSearch =
        user.displayName.toLowerCase().includes(searchLower) ||
        user.email.toLowerCase().includes(searchLower) ||
        (user.contactNumber && user.contactNumber.toLowerCase().includes(searchLower));

      if (!matchesSearch) return false;

      if (statusFilter === "PENDING") {
        return !user.active;
      }
      if (statusFilter === "ACTIVE") {
        return user.active;
      }
      return true;
    })
    .sort((a, b) => {
      // Sort inactive (pending approval) users to the top
      if (a.active !== b.active) {
        return a.active ? 1 : -1;
      }
      return a.displayName.localeCompare(b.displayName);
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
    <Card sx={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <CardContent sx={{ flexGrow: 1, display: "flex", flexDirection: "column", gap: 2 }}>
        
        {/* Header Title & Button */}
        <CardHeading
          title={title}
          subtitle={description}
          icon={<PeopleAltOutlinedIcon />}
          color="#0284c7"
          sx={{ mb: 0, alignItems: "flex-start" }}
          action={
            <Button
              variant="contained"
              color="primary"
              size="small"
              startIcon={<AddIcon />}
              onClick={onAddClick}
              sx={{ whiteSpace: "nowrap" }}
            >
              {addButtonLabel}
            </Button>
          }
        />

        {/* Search Bar & Status Filter */}
        <Box sx={{ display: "flex", gap: 1.5, flexWrap: "wrap" }}>
          <TextField
            size="small"
            placeholder="Search by name, email, or phone..."
            value={searchQuery}
            onChange={handleSearchChange}
            sx={{ flex: "1 1 220px", minWidth: 0 }}
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon fontSize="small" color="action" />
                  </InputAdornment>
                ),
              }
            }}
          />
          <FormControl size="small" sx={{ flex: "0 1 200px", minWidth: 160 }}>
            <InputLabel id="status-filter-label">Status Filter</InputLabel>
            <Select
              labelId="status-filter-label"
              id="status-filter"
              label="Status Filter"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
            >
              <MenuItem value="ALL">All Statuses</MenuItem>
              <MenuItem value="PENDING">Pending Approval (Inactive)</MenuItem>
              <MenuItem value="ACTIVE">Active Accounts</MenuItem>
            </Select>
          </FormControl>
        </Box>

        {/* Users Table */}
        <TableContainer component={Paper} variant="outlined" sx={{ overflowX: "auto" }}>
          <Table size="small" aria-label={`${title} table`} sx={{ minWidth: 500 }}>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Contact</TableCell>
                <TableCell>Last Login</TableCell>
                <TableCell align="center">Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {paginatedUsers.map((user) => (
                <TableRow key={user.userId} hover>
                  <TableCell>
                    <Box sx={{ display: "flex", flexDirection: "column" }}>
                      <Box sx={{ display: "flex", alignItems: "center", gap: 1, flexWrap: "wrap" }}>
                        <Typography variant="body2" sx={{ fontWeight: 600, color: "text.primary" }}>
                          {user.displayName}
                        </Typography>
                        {!user.active && (
                          <Chip
                            label="Pending"
                            size="small"
                            color="warning"
                            variant="outlined"
                            sx={{ height: 20, fontSize: "0.65rem", fontWeight: 700, borderColor: "#f59e0b", bgcolor: "#fffbeb" }}
                          />
                        )}
                      </Box>
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
                  <TableCell sx={{ whiteSpace: "nowrap" }}>
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
                  <TableCell colSpan={4} sx={{ py: 2 }}>
                    <EmptyState
                      message="No accounts found."
                      icon={<PersonSearchOutlinedIcon />}
                      minHeight={120}
                    />
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
