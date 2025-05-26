#pragma once

#include <vector>

struct Grid {
	const std::vector<std::string>& m_data;
	const size_t m_rowCount;
	const size_t m_colCount;

	Grid(const std::vector<std::string> &data) : m_data(data), m_rowCount(data.size()), m_colCount(data[0].size()) {}

	size_t index(size_t row, size_t col) const {
		return row * m_colCount + col;
	}

	size_t size() const { 
		return m_rowCount * m_colCount;
	}

	size_t row(size_t ravelIndex) const {
		return (ravelIndex - col(ravelIndex)) / m_colCount;
	}

	size_t col(size_t ravelIndex) const {
		return ravelIndex % m_colCount;
	}
};

